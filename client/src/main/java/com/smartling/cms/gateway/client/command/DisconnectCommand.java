package com.smartling.cms.gateway.client.command;

/**
 * Notifies client of immediate disconnect.
 */
public class DisconnectCommand extends BaseCommand
{
    private final String reasonMessage;

    public DisconnectCommand(String reasonMessage)
    {
        super(Type.DISCONNECT);
        this.reasonMessage = reasonMessage;
    }

    @Override
    String getCommandName()
    {
        return "disconnect";
    }

    public String getReasonMessage()
    {
        return reasonMessage;
    }
}
