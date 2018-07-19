package com.georlegacy.general.vestrimu.commands;

import com.georlegacy.general.vestrimu.App;
import com.georlegacy.general.vestrimu.core.Command;
import com.georlegacy.general.vestrimu.core.managers.SQLManager;
import com.georlegacy.general.vestrimu.core.managers.WebhookManager;
import com.georlegacy.general.vestrimu.core.objects.enumeration.CommandAccessType;
import com.georlegacy.general.vestrimu.util.Constants;
import com.georlegacy.general.vestrimu.util.URLUtil;
import com.google.inject.Inject;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Icon;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookClientBuilder;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class WebhookCommand extends Command {

    @Inject private SQLManager sqlManager;

    public WebhookCommand() {
        super(new String[]{"wh", "webhook", "sendwebhook"}, "Sends webhooks to the server.", "<channel> <args>", CommandAccessType.SERVER_ADMIN, true);
    }

    @Override
    public void execute(MessageReceivedEvent event) {
        Message message = event.getMessage();
        MessageChannel channel = event.getChannel();
        ArrayList<String> args = new ArrayList<String>(Arrays.asList(message.getContentRaw().split(" ")));
        args.remove(0);

        EmbedBuilder argumentsHelp = new EmbedBuilder();
        argumentsHelp
                .setTitle("Arguments")
                .setDescription("Below are all arguments for the webhook command, each of which begins with `--` and their values are supplied as like below **after the mentioned channel to send to** ```--plainMessage:This is the plain text before the embed```*Note: Do not use `--` within the value for any arguments*")
                .setColor(Constants.VESTRIMU_PURPLE)
                .setFooter("Vestrimu", Constants.ICON_URL)
                .addField("--avatarUrl", "The URL for the avatar of the webhook when the message is sent", true)
                .addField("--webhookName", "The name of the webhook when the message is sent", true)
                .addField("--plainMessage", "The plain message sent by the webhook before any embed content", true)
                .addField("--embedTitle", "The title of the (optional) embedded message following the plain message", true)
                .addField("--embedDescription", "The description of the (optional) embedded message following the plain message", true)
                .addField("--embedFooterText", "The text in the footer for the (optional) embedded message following the plain message", true)
                .addField("--embedFooterIconUrl", "The URL of the icon for the footer of the (optional) embedded message following the plain message", true)
                .addField("--embedThumbnailUrl", "The URL of the thumbnail of the (optional) embedded message following the plain message", true)
                .addField("--embedImageUrl", "The URL of the image of the (optional) embedded message following the plain message", true)
                .addField("--embedAuthorName", "The name of the author displayed on the (optional) embedded message following the plain message", true)
                .addField("--embedAuthorIconUrl", "The URL of the icon of the author displayed on the (optional) embedded message following the plain message", true)
                .addField("--fieldsInline", "A boolean value denoting whether all fields in the (optional) embedded message following the plain message will be inline", true)
                .addField("--fieldName", "The name of a field in the (optional) embedded message following the plain message, must be followed with a --fieldValue, both can be used several times", true)
                .addField("--fieldValue", "The value of a field in the (optional) embedded message following the plain message, must follow a --fieldName", true);


        if (args.size() == 0) {
            EmbedBuilder eb = new EmbedBuilder();
            eb
                    .setTitle("Sorry")
                    .setDescription("You need to provide a channel and arguments, for information on arguments, follow this command with `arguments`")
                    .setColor(Constants.VESTRIMU_PURPLE)
                    .setFooter("Vestrimu", Constants.ICON_URL);
            channel.sendMessage(eb.build()).queue();
            return;
        }

        if (args.get(0).equalsIgnoreCase("arguments") || args.get(0).equalsIgnoreCase("args")) {
            channel.sendMessage(argumentsHelp.build()).queue();
            return;
        }

        List<TextChannel> mentioned  = message.getMentionedChannels();
        if (mentioned.size() == 0) {
            EmbedBuilder eb = new EmbedBuilder();
            eb
                    .setTitle("Sorry")
                    .setDescription("You need to mention a channel to send the webhook to")
                    .setColor(Constants.VESTRIMU_PURPLE)
                    .setFooter("Vestrimu", Constants.ICON_URL);
            channel.sendMessage(eb.build()).queue();
            return;
        }

        if (!args.get(0).equals(mentioned.get(0).getAsMention())) {
            EmbedBuilder eb = new EmbedBuilder();
            eb
                    .setTitle("Sorry")
                    .setDescription("The channel to send the webhook to must directly follow the command")
                    .setColor(Constants.VESTRIMU_PURPLE)
                    .setFooter("Vestrimu", Constants.ICON_URL);
            channel.sendMessage(eb.build()).queue();
            return;
        }

        args.remove(0);

        String avatarUrl = null;
        String webhookName = null;
        String plainMessage = null;
        String embedTitle = null;
        String embedDescription = null;
        String embedFooterText = null;
        String embedFooterIconUrl = null;
        String embedThumbnailUrl = null;
        String embedImageUrl = null;
        String embedAuthorName = null;
        String embedAuthorIconUrl = null;
        boolean fieldsInline = true;
        HashMap</* Name */ String, /* Value */String> fields = new HashMap<String, String>();

        ArrayList<String> newArgs = new ArrayList<String>(Arrays.asList(String.join(" ", args).trim().split("--")));

        if (newArgs.size() == 1) {
            EmbedBuilder eb = new EmbedBuilder();
            eb
                    .setTitle("Sorry")
                    .setDescription("You need to provide further arguments, for more information on arguments, run this command followed by `arguments`")
                    .setColor(Constants.VESTRIMU_PURPLE)
                    .setFooter("Vestrimu", Constants.ICON_URL);
            channel.sendMessage(eb.build()).queue();
            return;
        }

        newArgs.remove(0);

        String currentFieldName = null;
        for (String arg : newArgs) {
            if (arg.startsWith("avatarUrl:")) {
                if (arg.equals("avatarUrl:")) {
                    sendNoValue(arg, channel);
                    return;
                }
                avatarUrl = arg.replaceFirst("avatarUrl:", "").trim();
                continue;
            }
            if (arg.startsWith("webhookName:")) {
                if (arg.equals("webhookName:")) {
                    sendNoValue(arg, channel);
                    return;
                }
                webhookName = arg.replaceFirst("webhookName:", "").trim();
                continue;
            }
            if (arg.startsWith("plainMessage:")) {
                if (arg.equals("plainMessage:")) {
                    sendNoValue(arg, channel);
                    return;
                }
                plainMessage = arg.replaceFirst("plainMessage:", "").trim();
                continue;
            }
            if (arg.startsWith("embedTitle:")) {
                if (arg.equals("embedTitle:")) {
                    sendNoValue(arg, channel);
                    return;
                }
                embedTitle = arg.replaceFirst("embedTitle:", "").trim();
                continue;
            }
            if (arg.startsWith("embedDescription:")) {
                if (arg.equals("embedDescription:")) {
                    sendNoValue(arg, channel);
                    return;
                }
                embedDescription = arg.replaceFirst("embedDescription:", "").trim();
                continue;
            }
            if (arg.startsWith("embedFooterText:")) {
                if (arg.equals("embedFooterText:")) {
                    sendNoValue(arg, channel);
                    return;
                }
                embedFooterText = arg.replaceFirst("embedFooterText:", "").trim();
                continue;
            }
            if (arg.startsWith("embedFooterIconUrl:")) {
                if (arg.equals("embedFooterIconUrl:")) {
                    sendNoValue(arg, channel);
                    return;
                }
                embedFooterIconUrl = arg.replaceFirst("embedFooterIconUrl:", "").trim();
                continue;
            }
            if (arg.startsWith("embedThumbnailUrl:")) {
                if (arg.equals("embedThumbnailUrl:")) {
                    sendNoValue(arg, channel);
                    return;
                }
                embedThumbnailUrl = arg.replaceFirst("embedThumbnailUrl:", "").trim();
                continue;
            }
            if (arg.startsWith("embedImageUrl:")) {
                if (arg.equals("embedImageUrl:")) {
                    sendNoValue(arg, channel);
                    return;
                }
                embedImageUrl = arg.replaceFirst("embedImageUrl:", "").trim();
                continue;
            }
            if (arg.startsWith("embedAuthorName:")) {
                if (arg.equals("embedAuthorName:")) {
                    sendNoValue(arg, channel);
                    return;
                }
                embedAuthorName = arg.replaceFirst("embedAuthorName:", "").trim();
                continue;
            }
            if (arg.startsWith("embedAuthorIconUrl:")) {
                if (arg.equals("embedAuthorIconUrl:")) {
                    sendNoValue(arg, channel);
                    return;
                }
                embedAuthorIconUrl = arg.replaceFirst("embedAuthorIconUrl:", "").trim();
                continue;
            }
            if (arg.startsWith("fieldsInline:")) {
                if (arg.equals("fieldsInline:")) {
                    sendNoValue(arg, channel);
                    return;
                }
                try {
                    fieldsInline = Boolean.parseBoolean(arg.replaceFirst("fieldsInline:", "").trim());
                } catch (IllegalArgumentException ex) {
                    EmbedBuilder eb = new EmbedBuilder();
                    eb
                            .setTitle("Sorry")
                            .setDescription("That doesn't appear to be a valid boolean value")
                            .setColor(Constants.VESTRIMU_PURPLE)
                            .setFooter("Vestrimu", Constants.ICON_URL);
                    channel.sendMessage(eb.build()).queue();
                    return;
                }
                continue;
            }
            if (arg.startsWith("fieldName:")) {
                if (arg.equals("fieldName:")) {
                    sendNoValue(arg, channel);
                    return;
                }
                currentFieldName = arg.replaceFirst("fieldName:", "").trim();
                continue;
            }
            if (arg.startsWith("fieldValue:")) {
                if (arg.equals("fieldValue:")) {
                    sendNoValue(arg, channel);
                    return;
                }
                if (currentFieldName == null) {
                    EmbedBuilder eb = new EmbedBuilder();
                    eb
                            .setTitle("Sorry")
                            .setDescription("You need to provide a `--fieldName:` argument before a `--fieldValue:` argument")
                            .setColor(Constants.VESTRIMU_PURPLE)
                            .setFooter("Vestrimu", Constants.ICON_URL);
                    channel.sendMessage(eb.build()).queue();
                    return;
                } else {
                    fields.put(currentFieldName, arg.replaceFirst("fieldValue:", "").trim());
                    currentFieldName = null;
                }
                continue;
            }

            EmbedBuilder eb = new EmbedBuilder();
            eb
                    .setTitle("Sorry")
                    .setDescription("`" + arg + "` is not a valid argument for this command, to view valid arguments, use `" + sqlManager.readGuild(event.getGuild().getId()).getPrefix() + "webhook arguments`")
                    .setColor(Constants.VESTRIMU_PURPLE)
                    .setFooter("Vestrimu", Constants.ICON_URL);
            channel.sendMessage(eb.build()).queue();
            return;
        }

        if (currentFieldName != null) {
            EmbedBuilder eb = new EmbedBuilder();
            eb
                    .setTitle("Sorry")
                    .setDescription("You have provided a name for a field but haven't provided a subsequent value for said field")
                    .setColor(Constants.VESTRIMU_PURPLE)
                    .setFooter("Vestrimu", Constants.ICON_URL);
            channel.sendMessage(eb.build()).queue();
            return;
        }

        if (((embedFooterText == null) && (embedFooterIconUrl != null)) || ((embedFooterText != null) && (embedFooterIconUrl == null))) {
            EmbedBuilder eb = new EmbedBuilder();
            eb
                    .setTitle("Sorry")
                    .setDescription("If you provide either a footer icon URL or text for the footer of the embed, you must provide both")
                    .setColor(Constants.VESTRIMU_PURPLE)
                    .setFooter("Vestrimu", Constants.ICON_URL);
            channel.sendMessage(eb.build()).queue();
            return;
        }

        if ((embedAuthorIconUrl != null) && (embedAuthorName == null)) {
            EmbedBuilder eb = new EmbedBuilder();
            eb
                    .setTitle("Sorry")
                    .setDescription("If you provide an icon URL for the author of the embed, you must provide a name for the author")
                    .setColor(Constants.VESTRIMU_PURPLE)
                    .setFooter("Vestrimu", Constants.ICON_URL);
            channel.sendMessage(eb.build()).queue();
            return;
        }

        if (!URLUtil.validateURL(avatarUrl) || !URLUtil.validateURL(embedImageUrl) || !URLUtil.validateURL(embedThumbnailUrl) || !URLUtil.validateURL(embedAuthorIconUrl) || !URLUtil.validateURL(embedFooterIconUrl)) {
            EmbedBuilder eb = new EmbedBuilder();
            eb
                    .setTitle("Sorry")
                    .setDescription("One or more of your URLs is malformed, please check them and try again")
                    .setColor(Constants.VESTRIMU_PURPLE)
                    .setFooter("Vestrimu", Constants.ICON_URL);
            channel.sendMessage(eb.build()).queue();
            return;
        }

        final String avatarUrlFinal = avatarUrl;
        final String webhookNameFinal = webhookName;
        final String plainMessageFinal = plainMessage;
        final String embedTitleFinal = embedTitle;
        final String embedDescriptionFinal = embedDescription;
        final String embedFooterTextFinal = embedFooterText;
        final String embedFooterIconUrlFinal = embedFooterIconUrl;
        final String embedThumbnailUrlFinal = embedThumbnailUrl;
        final String embedImageUrlFinal = embedImageUrl;
        final String embedAuthorNameFinal = embedAuthorName;
        final String embedAuthorIconUrlFinal = embedAuthorIconUrl;
        final boolean fieldsInlineFinal = fieldsInline;
        final HashMap<String, String> fieldsFinal = fields;

        event.getGuild().getWebhooks().queue(webhooks -> webhooks.forEach(webhook -> {
            if (webhook.getId().equals(sqlManager.readGuild(event.getGuild().getId()).getPrimarywebhookid())) {
                EmbedBuilder embed = new EmbedBuilder();
                if (embedTitleFinal != null)
                    embed.setTitle(embedTitleFinal);
                if (embedDescriptionFinal != null)
                    embed.setDescription(embedDescriptionFinal);
                if (embedFooterTextFinal != null)
                    embed.setFooter(embedFooterTextFinal, embedFooterIconUrlFinal);
                if (embedThumbnailUrlFinal != null)
                    embed.setThumbnail(embedThumbnailUrlFinal);
                if (embedImageUrlFinal != null)
                    embed.setImage(embedImageUrlFinal);
                if (embedAuthorNameFinal != null)
                    if (embedAuthorIconUrlFinal != null)
                        embed.setAuthor(embedAuthorNameFinal, "https://discordapp.com", embedAuthorIconUrlFinal);
                    else
                        embed.setAuthor(embedAuthorNameFinal);
                for (Map.Entry<String, String> field : fieldsFinal.entrySet())
                    embed.addField(field.getKey(), field.getValue(), fieldsInlineFinal);

                WebhookClient client = webhook.newClient().build();
                WebhookMessageBuilder builder = new WebhookMessageBuilder();
                if (plainMessageFinal != null)
                    builder.append(plainMessageFinal);
                if (!embed.isEmpty())
                    builder.addEmbeds(embed.build());

                try {
                    webhook.getManager().setName(webhookNameFinal == null ? "Vestrimu" : webhookNameFinal).queue();
                    HttpsURLConnection con = (HttpsURLConnection) new URL(avatarUrlFinal).openConnection();
                    con.addRequestProperty("User-Agent", "Mozilla/4.76");
                    webhook.getManager().setAvatar(avatarUrlFinal == null ? Icon.from(App.class.getClassLoader().getResourceAsStream("icon.png")) : Icon.from(con.getInputStream())).queue();
                    webhook.getManager().setChannel(mentioned.get(0)).queue(consumer -> {
                            client.send(builder.build());
                            client.close();
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }

                webhook.getManager().setName("Vestrimu Primary Webhook").queue();
                try {
                    webhook.getManager().setAvatar(Icon.from(App.class.getClassLoader().getResourceAsStream("icon.png"))).queue();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
            EmbedBuilder eb = new EmbedBuilder();
            eb
                    .setTitle("Sorry")
                    .setDescription("An error occurred whilst attempting to send the message via webhook.")
                    .setColor(Constants.VESTRIMU_PURPLE)
                    .setFooter("Vestrimu", Constants.ICON_URL);
            channel.sendMessage(eb.build());
        }));

    }

    private void sendNoValue(String arg, MessageChannel channel) {
        EmbedBuilder eb = new EmbedBuilder();
        eb
                .setTitle("Sorry")
                .setDescription("You need to provide a value for the argument `" + arg + "`")
                .setColor(Constants.VESTRIMU_PURPLE)
                .setFooter("Vestrimu", Constants.ICON_URL);
        channel.sendMessage(eb.build()).queue();
    }

}
