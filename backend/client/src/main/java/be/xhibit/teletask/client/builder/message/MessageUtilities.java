package be.xhibit.teletask.client.builder.message;

import be.xhibit.teletask.client.TDSClient;
import be.xhibit.teletask.client.builder.ByteUtilities;
import be.xhibit.teletask.client.builder.composer.MessageHandler;
import be.xhibit.teletask.client.builder.message.response.AcknowledgeServerResponse;
import be.xhibit.teletask.client.builder.message.response.EventMessageServerResponse;
import be.xhibit.teletask.client.builder.message.response.ServerResponse;
import be.xhibit.teletask.model.spec.ClientConfigSpec;
import be.xhibit.teletask.model.spec.ComponentSpec;
import com.google.common.primitives.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class MessageUtilities {
    /**
     * Logger responsible for logging and debugging statements.
     */
    private static final Logger LOG = LoggerFactory.getLogger(MessageUtilities.class);

    private static final Collection<EventMessage> TEST_EVENTS = new ConcurrentLinkedQueue<>();


    private MessageUtilities() {
    }

    public static <T> T receive(Class origin, InputStream inputStream, ClientConfigSpec config, MessageHandler messageHandler, StopCondition stopCondition, ResponseConverter<T> converter) throws Exception {
        return converter.convert(TDSClient.isProduction() ? receiveProduction(origin, inputStream, config, messageHandler, stopCondition) : receiveTest());
    }

    private static List<ServerResponse> receiveTest() {
        List<ServerResponse> responses = new ArrayList<>();
        for (EventMessage eventMessage : TEST_EVENTS) {
            responses.add(new EventMessageServerResponse(eventMessage));
        }
        TEST_EVENTS.clear();
        return responses;
    }

    private static List<ServerResponse> receiveProduction(Class origin, InputStream inputStream, ClientConfigSpec config, MessageHandler messageHandler, StopCondition stopCondition) throws Exception {
        List<ServerResponse> responses = new ArrayList<>();

        byte[] overflow = new byte[1];
        long startTime = System.currentTimeMillis();
        while (!stopCondition.isComplete(responses, overflow)) {
            if ((System.currentTimeMillis() - startTime) > 5000) {
                throw new RuntimeException("Did not receive data in a timely fashion. This means either: \n\t- You sent wrong data to the server and hence did not get an acknowledge.\n\t- Or you requested information from the server that was not available to the server");
            }
            int available = inputStream.available();
            if (available > 0) {
                byte[] read = new byte[available];
                inputStream.read(read, 0, available);
                byte[] data = Bytes.concat(overflow, read);
                overflow = extractResponses(origin, config, messageHandler, responses, data);
            } else {
                overflow = new byte[0];
            }
            Thread.sleep(10);
        }

        return responses;
    }

    public static void registerTestEvent(EventMessage message) {
        LOG.debug("Registering test event: {}", message);
        TEST_EVENTS.add(message);
    }

    private static byte[] extractResponses(Class origin, ClientConfigSpec config, MessageHandler messageHandler, Collection<ServerResponse> responses, byte[] data) throws Exception {
        LOG.debug("Receive({}) - Raw bytes: {}", origin.getSimpleName(), ByteUtilities.bytesToHex(data));
        byte[] overflow = new byte[0];
        for (int i = 0; i < data.length; i++) {
            byte b = data[i];
            LOG.debug("Receive({}) - Processing byte: {}", origin.getSimpleName(), ByteUtilities.bytesToHex(b));
            if (b == messageHandler.getStxValue()) {
                int eventLengthInclChkSum = data[i + 1] + 1; // +1 for checksum
                byte[] event = new byte[eventLengthInclChkSum];

                if (i + eventLengthInclChkSum > data.length) {
                    overflow = new byte[data.length - i];
                    System.arraycopy(data, i, event, 0, data.length - i);
                    i = data.length - 1;

                    LOG.debug("Receive({}) - Overflowing following byte[]: {}", origin.getSimpleName(), ByteUtilities.bytesToHex(overflow));
                } else {
                    System.arraycopy(data, i, event, 0, eventLengthInclChkSum);

                    i += eventLengthInclChkSum - 1;

                    LOG.debug("Receive({}) - Found event bytes: {}", origin.getSimpleName(), ByteUtilities.bytesToHex(event));
                    try {
                        responses.add(new EventMessageServerResponse(messageHandler.parseEvent(config, event)));
                    } catch (Exception e) {
                        LOG.error("Exception ({}) caught in readLogResponse: {}", e.getClass().getName(), e.getMessage(), e);
                    }
                }
            } else if (b == messageHandler.getAcknowledgeValue()) {
                responses.add(new AcknowledgeServerResponse());
            } else {
                LOG.warn("Receive({}) - Found byte, but don't know how to handle it: {}", origin.getSimpleName(), ByteUtilities.bytesToHex(b));
            }
        }
        return overflow;
    }

    public static void send(OutputStream outputStream, byte[] message) throws IOException {
        outputStream.write(message);
        outputStream.flush();
    }

    public static ComponentSpec handleEvent(Class origin, ClientConfigSpec config, EventMessageServerResponse serverResponse) {
        EventMessage eventMessage = serverResponse.getEventMessage();
        if (LOG.isDebugEnabled() && TDSClient.isProduction()) {
            LOG.debug("Event({}): {}", origin.getSimpleName(), eventMessage.getLogInfo(eventMessage.getRawBytes()));
        }
        ComponentSpec component = config.getComponent(eventMessage.getFunction(), eventMessage.getNumber());
        component.setState(eventMessage.getState());
        return component;
    }

    public interface StopCondition {
        boolean isComplete(List<ServerResponse> responses, byte[] overflow);
    }

    public interface ResponseConverter<T> {
        T convert(List<ServerResponse> responses);
    }
}
