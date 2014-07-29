package be.xhibit.teletask.client.builder.composer.v3_1;

import be.xhibit.teletask.client.TDSClient;
import be.xhibit.teletask.client.builder.composer.MessageHandlerSupport;
import be.xhibit.teletask.client.builder.composer.config.configurables.StateKey;
import be.xhibit.teletask.client.builder.message.EventMessage;
import be.xhibit.teletask.client.builder.message.GroupGetMessage;
import be.xhibit.teletask.client.builder.message.KeepAliveMessage;
import be.xhibit.teletask.client.builder.message.MessageExecutor;
import be.xhibit.teletask.client.builder.message.MessageUtilities;
import be.xhibit.teletask.client.builder.message.strategy.GroupGetStrategy;
import be.xhibit.teletask.client.builder.message.strategy.KeepAliveStrategy;
import be.xhibit.teletask.model.spec.ClientConfigSpec;
import be.xhibit.teletask.model.spec.Command;
import be.xhibit.teletask.model.spec.ComponentSpec;
import be.xhibit.teletask.model.spec.Function;
import be.xhibit.teletask.model.spec.State;
import com.google.common.primitives.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;

public class MicrosPlusMessageHandler extends MessageHandlerSupport {
    /**
     * Logger responsible for logging and debugging statements.
     */
    private static final Logger LOG = LoggerFactory.getLogger(MicrosPlusMessageHandler.class);
    public static final MicrosPlusKeepAliveStrategy KEEP_ALIVE_STRATEGY = new MicrosPlusKeepAliveStrategy();
    public static final MicrosPlusGroupGetStrategy GROUP_GET_STRATEGY = new MicrosPlusGroupGetStrategy();

    public MicrosPlusMessageHandler() {
        super(new MicrosPlusCommandConfiguration(), new MicrosPlusStateConfiguration(), new MicrosPlusFunctionConfiguration());
    }

    @Override
    public byte[] compose(Command command, byte[] payload) {
        boolean cuParam = this.getCommandConfig(command).needsCentralUnitParameter();

        int msgStx = this.getStxValue();                                    // STX: is this value always fixed 02h?
        int msgLength = (cuParam ? 4 : 3) + payload.length;                                 // Length: the length of the command without checksum
        int msgCommand = this.getCommandConfig(command).getNumber();        // Command Number
        int msgCentralUnit = 1;                                             // Now we only support 1 central unit per

        byte[] begin = {(byte) msgStx, (byte) msgLength, (byte) msgCommand};
        byte[] messageBytes = null;
        if (cuParam) {
            messageBytes = Bytes.concat(begin, new byte[]{(byte) msgCentralUnit}, payload);
        } else {
            messageBytes = Bytes.concat(begin, payload);
        }

        return this.addCheckSum(messageBytes);
    }

    @Override
    public byte[] composeOutput(int... numbers) {
        byte[] outputs = new byte[numbers.length * 2];
        for (int i = 0; i < numbers.length; i++) {
            byte[] bytes = ByteBuffer.allocate(2).putShort((short) numbers[i]).array();
            outputs[i * 2] = bytes[0];
            outputs[(i * 2) + 1] = bytes[1];
        }
        return outputs;
    }

    @Override
    public EventMessage parseEvent(ClientConfigSpec config, byte[] eventData) {
        //02 09 10 01 01 00 03 00 00 20
        int counter = 3; //We skip first 4 since they are of no use to us at this time.
        Function function = this.getFunction(eventData[++counter]);
        int number = ByteBuffer.wrap(new byte[]{eventData[++counter], eventData[++counter]}).getShort();
        ++counter; // This is the ErrorState, not used at this time
        int stateValue = eventData[++counter];
        State state = this.getState(new StateKey(function, stateValue == -1 ? 255 : stateValue));

        return this.createEventMessage(config, eventData, function, number, state);
    }

    @Override
    public String getOutputLogHeaderName(int index) {
        return "Output Part " + (((index + 1) % 2) + 1);
    }

    @Override
    public KeepAliveStrategy getKeepAliveStrategy() {
        return KEEP_ALIVE_STRATEGY;
    }

    @Override
    public GroupGetStrategy getGroupGetStrategy() {
        return GROUP_GET_STRATEGY;
    }

    private static class MicrosPlusKeepAliveStrategy implements KeepAliveStrategy {
        @Override
        public int getIntervalMinutes() {
            return 3;
        }

        @Override
        public void execute(ClientConfigSpec config, OutputStream out, InputStream in) throws Exception {
            MessageExecutor.of(new KeepAliveMessage(config), out, in).call();
        }
    }

    private static class MicrosPlusGroupGetStrategy implements GroupGetStrategy {
        @Override
        public List<ComponentSpec> execute(ClientConfigSpec config, OutputStream out, InputStream in, Function function, int... numbers) throws Exception {
            return MessageExecutor.of(new GroupGetMessage(config, function, numbers), out, in).call();
        }
    }
}
