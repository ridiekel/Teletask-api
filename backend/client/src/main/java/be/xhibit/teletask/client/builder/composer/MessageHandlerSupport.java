package be.xhibit.teletask.client.builder.composer;

import be.xhibit.teletask.client.TDSClient;
import be.xhibit.teletask.client.builder.composer.config.ConfigurationSupport;
import be.xhibit.teletask.client.builder.composer.config.configurables.CommandConfigurable;
import be.xhibit.teletask.client.builder.composer.config.configurables.FunctionConfigurable;
import be.xhibit.teletask.client.builder.composer.config.configurables.StateConfigurable;
import be.xhibit.teletask.client.builder.composer.config.configurables.StateKey;
import be.xhibit.teletask.client.builder.message.EventMessage;
import be.xhibit.teletask.client.builder.message.MessageUtilities;
import be.xhibit.teletask.model.spec.ClientConfigSpec;
import be.xhibit.teletask.model.spec.Command;
import be.xhibit.teletask.model.spec.Function;
import be.xhibit.teletask.model.spec.State;
import com.google.common.primitives.Bytes;

public abstract class MessageHandlerSupport implements MessageHandler {
    private final ConfigurationSupport<Command, CommandConfigurable, Integer> commandConfiguration;
    private final ConfigurationSupport<State, StateConfigurable, StateKey> stateConfiguration;
    private final ConfigurationSupport<Function, FunctionConfigurable, Integer> functionConfiguration;

    protected MessageHandlerSupport(ConfigurationSupport<Command, CommandConfigurable, Integer> commandConfiguration, ConfigurationSupport<State, StateConfigurable, StateKey> stateConfiguration, ConfigurationSupport<Function, FunctionConfigurable, Integer> functionConfiguration) {
        this.commandConfiguration = commandConfiguration;
        this.stateConfiguration = stateConfiguration;
        this.functionConfiguration = functionConfiguration;
    }

    protected byte[] addCheckSum(byte[] messageBytes) {
        // ChkSm: Command Number + Command Parameters + Length + STX
        byte checkSumByte = 0;
        for (byte messageByte : messageBytes) {
            checkSumByte += messageByte;
        }
        messageBytes = Bytes.concat(messageBytes, new byte[]{checkSumByte});
        return messageBytes;
    }

    @Override
    public int getStxValue() {
        return 2;
    }

    protected EventMessage createEventMessage(ClientConfigSpec config, byte[] eventData, Function function, int number, State state) {
        return new EventMessage(config, eventData, function, number, state);
    }

    @Override
    public String getOutputLogHeaderName(int index) {
        return "Output";
    }

    @Override
    public int getAcknowledgeValue() {
        return 10;
    }

    @Override
    public CommandConfigurable getCommandConfig(Command command) {
        return this.getCommandConfiguration().getConfigurable(command);
    }

    @Override
    public StateConfigurable getStateConfig(State state) {
        return this.getStateConfiguration().getConfigurable(state);
    }

    @Override
    public FunctionConfigurable getFunctionConfig(Function function) {
        return this.getFunctionConfiguration().getConfigurable(function);
    }

    public ConfigurationSupport<Command, CommandConfigurable, Integer> getCommandConfiguration() {
        return this.commandConfiguration;
    }

    public ConfigurationSupport<State, StateConfigurable, StateKey> getStateConfiguration() {
        return this.stateConfiguration;
    }

    public ConfigurationSupport<Function, FunctionConfigurable, Integer> getFunctionConfiguration() {
        return this.functionConfiguration;
    }

    @Override
    public Function getFunction(int function) {
        return this.getFunctionConfiguration().getConfigObject(function);
    }

    @Override
    public Command getCommand(int command) {
        return this.getCommandConfiguration().getConfigObject(command);
    }

    @Override
    public State getState(StateKey key) {
        return this.getStateConfiguration().getConfigObject(key);
    }

    @Override
    public boolean knows(Command command) {
        return this.getCommandConfiguration().knows(command);
    }

    @Override
    public boolean knows(Function function) {
        return this.getFunctionConfiguration().knows(function);
    }

    @Override
    public boolean knows(State state) {
        return this.getStateConfiguration().knows(state);
    }
}
