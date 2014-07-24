package be.xhibit.teletask.client.builder.composer.v2_8;

import be.xhibit.teletask.client.TDSClient;
import be.xhibit.teletask.client.builder.CommandConfig;
import be.xhibit.teletask.client.builder.FunctionConfig;
import be.xhibit.teletask.client.builder.StateConfig;
import be.xhibit.teletask.client.builder.composer.MessageHandlerSupport;
import be.xhibit.teletask.client.builder.message.EventMessage;
import be.xhibit.teletask.model.spec.Command;
import be.xhibit.teletask.model.spec.Function;
import be.xhibit.teletask.model.spec.State;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MicrosMessageHandler extends MessageHandlerSupport {
    /**
     * Logger responsible for logging and debugging statements.
     */
    private static final Logger LOG = LoggerFactory.getLogger(MicrosMessageHandler.class);

    public MicrosMessageHandler() {
        super(ImmutableMap.<Command, CommandConfig>builder()
                        .put(Command.SET, new CommandConfig(1, "Fnc", "Outp", "Sate"))
                        .put(Command.GET, new CommandConfig(2, "Fnc", "Outp"))
                        .put(Command.LOG, new CommandConfig(3, "Fnc", "Sate"))
                        .put(Command.EVENT, new CommandConfig(8, "Fnc", "Outp", "Sate"))
                        .build(),
                ImmutableMap.<State, StateConfig>builder()
                        .put(State.ON, new StateConfig(255))
                        .put(State.OFF, new StateConfig(0))
                        .put(State.UP, new StateConfig(255))
                        .put(State.DOWN, new StateConfig(0))
                        .build(),
                ImmutableMap.<Function, FunctionConfig>builder()
                        .put(Function.RELAY, new FunctionConfig(1))
                        .put(Function.DIMMER, new FunctionConfig(2))
                        .put(Function.MOTOR, new FunctionConfig(55))
                        .put(Function.LOCMOOD, new FunctionConfig(8))
                        .put(Function.TIMEDMOOD, new FunctionConfig(9))
                        .put(Function.GENMOOD, new FunctionConfig(10))
                        .put(Function.FLAG, new FunctionConfig(15))
                        .put(Function.SENSOR, new FunctionConfig(20))
                        .put(Function.COND, new FunctionConfig(60))
                        .build()
        );
    }

    @Override
    public byte[] compose(Command command, byte[] payload) {
        int msgStx = this.getStart();                                       // STX: is this value always fixed 02h?
        int msgLength = 3 + payload.length;                                 // Length: the length of the command without checksum
        int msgCommand = this.getCommandConfig(command).getNumber();        // Command Number

        byte[] messageBytes = Bytes.concat(new byte[]{(byte) msgStx, (byte) msgLength, (byte) msgCommand}, payload);

        return this.getMessageWithChecksum(messageBytes);
    }

    @Override
    public byte[] composeOutput(int number) {
        return new byte[]{(byte) number};
    }

    @Override
    public void handleEvent(TDSClient client, byte[] eventData) {
        //02 09 10 01 03 00 31
        int counter = 2; //We skip first 3 since they are of no use to us at this time.
        Function function = this.getFunction(eventData[++counter]);
        int number = eventData[++counter];
        int stateValue = eventData[++counter];
        State state = this.getState(stateValue == -1 ? 255 : stateValue);
        EventMessage eventMessage = new EventMessage(client.getConfig(), function, number, state);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Handling event: {}", eventMessage.getLogInfo(eventData));
        }
        client.setState(function, number, state);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}