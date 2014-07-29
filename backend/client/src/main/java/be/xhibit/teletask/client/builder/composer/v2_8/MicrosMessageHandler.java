package be.xhibit.teletask.client.builder.composer.v2_8;

import be.xhibit.teletask.client.TDSClient;
import be.xhibit.teletask.client.builder.composer.MessageHandlerSupport;
import be.xhibit.teletask.client.builder.composer.config.configurables.StateKey;
import be.xhibit.teletask.client.builder.message.EventMessage;
import be.xhibit.teletask.client.builder.message.GetMessage;
import be.xhibit.teletask.client.builder.message.LogMessage;
import be.xhibit.teletask.client.builder.message.MessageExecutor;
import be.xhibit.teletask.client.builder.message.MessageUtilities;
import be.xhibit.teletask.client.builder.message.strategy.GroupGetStrategy;
import be.xhibit.teletask.client.builder.message.strategy.KeepAliveStrategy;
import be.xhibit.teletask.model.spec.ClientConfigSpec;
import be.xhibit.teletask.model.spec.Command;
import be.xhibit.teletask.model.spec.ComponentSpec;
import be.xhibit.teletask.model.spec.Function;
import be.xhibit.teletask.model.spec.State;
import be.xhibit.teletask.model.spec.StateEnum;
import com.google.common.primitives.Bytes;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MicrosMessageHandler extends MessageHandlerSupport {
    public static final MicrosKeepAliveStrategy KEEP_ALIVE_STRATEGY = new MicrosKeepAliveStrategy();
    public static final MicrosGroupGetStrategy GROUP_GET_STRATEGY = new MicrosGroupGetStrategy();

    public MicrosMessageHandler() {
        super(new MicrosCommandConfiguration(), new MicrosStateConfiguration(), new MicrosFunctionConfiguration());
    }

    @Override
    public byte[] compose(Command command, byte[] payload) {
        int msgStx = this.getStxValue();                                       // STX: is this value always fixed 02h?
        int msgLength = 3 + payload.length;                                 // Length: the length of the command without checksum
        int msgCommand = this.getCommandConfig(command).getNumber();        // Command Number

        byte[] messageBytes = Bytes.concat(new byte[]{(byte) msgStx, (byte) msgLength, (byte) msgCommand}, payload);

        return this.addCheckSum(messageBytes);
    }

    @Override
    public byte[] composeOutput(int... numbers) {
        byte[] outputs = new byte[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            outputs[i] = (byte) numbers[i];

        }
        return outputs;
    }

    @Override
    public EventMessage parseEvent(ClientConfigSpec config, byte[] eventData) {
        //02 09 10 01 03 00 31
        int counter = 2; //We skip first 3 since they are of no use to us at this time.
        Function function = this.getFunction(eventData[++counter]);
        int number = eventData[++counter];
        int stateValue = eventData[++counter];
        State state = this.getState(new StateKey(function, stateValue == -1 ? 255 : stateValue));
        return this.createEventMessage(config, eventData, function, number, state);
    }

    @Override
    public KeepAliveStrategy getKeepAliveStrategy() {
        return KEEP_ALIVE_STRATEGY;
    }

    @Override
    public GroupGetStrategy getGroupGetStrategy() {
        return GROUP_GET_STRATEGY;
    }

    private static class MicrosKeepAliveStrategy implements KeepAliveStrategy {
        @Override
        public int getIntervalMinutes() {
            return 30;
        }

        @Override
        public void execute(ClientConfigSpec config, OutputStream out, InputStream in) throws Exception {
            MessageExecutor.of(new LogMessage(config, Function.MOTOR, StateEnum.ON), out, in).call();
        }
    }

    private static class MicrosGroupGetStrategy implements GroupGetStrategy {
        @Override
        public List<ComponentSpec> execute(ClientConfigSpec config, OutputStream out, InputStream in, Function function, int... numbers) throws Exception {
            List<ComponentSpec> componentSpecs = new ArrayList<>();
            for (int number : numbers) {
                componentSpecs.add(MessageExecutor.of(new GetMessage(config, function, number), out, in).call());
            }
            return componentSpecs;
        }
    }
}
