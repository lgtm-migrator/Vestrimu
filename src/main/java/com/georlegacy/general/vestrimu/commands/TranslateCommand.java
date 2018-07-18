package com.georlegacy.general.vestrimu.commands;

import com.georlegacy.general.vestrimu.SecretConstants;
import com.georlegacy.general.vestrimu.core.Command;
import com.georlegacy.general.vestrimu.core.objects.enumeration.CommandAccessType;
import com.georlegacy.general.vestrimu.util.Constants;
import com.rmtheis.yandtran.TranslateUtils;
import com.rmtheis.yandtran.language.Language;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.Arrays;

public class TranslateCommand extends Command {

    public TranslateCommand() {
        super(new String[]{"translate"}, "Translates a message", "<language> <message>", CommandAccessType.USER_ANY, false);
    }

    @Override
    public void execute(MessageReceivedEvent event) {
        Message message = event.getMessage();
        MessageChannel channel = event.getChannel();
        ArrayList<String> args = new ArrayList<String>(Arrays.asList(message.getContentRaw().split(" ")));
        args.remove(0);
        if (args.size() == 0) {
            EmbedBuilder eb = new EmbedBuilder();
            eb
                    .setColor(Constants.VESTRIMU_PURPLE)
                    .setTitle("**Failed**")
                    .setDescription("You didn't provide a language or anything to translate, try using this format\n`translate " + getHelp() + "`");
            channel.sendMessage(eb.build()).queue();
            return;
        }
        if (args.size() == 1) {
            EmbedBuilder eb = new EmbedBuilder();
            eb
                    .setColor(Constants.VESTRIMU_PURPLE)
                    .setTitle("**Failed**")
                    .setDescription("You didn't provide anything to translate, try using this format\n`translate " + getHelp() + "`");
            channel.sendMessage(eb.build()).queue();
            return;
        }
        final Language finalLanguage;
        try {
            finalLanguage = Language.valueOf(args.get(0).toUpperCase());
        } catch (IllegalArgumentException ex) {
            EmbedBuilder eb = new EmbedBuilder();
            eb
                    .setColor(Constants.VESTRIMU_PURPLE)
                    .setTitle("**Failed**")
                    .setDescription("The language you provided (`" + args.get(0) + "`) is not a valid language. Find a list fo valid languages here\ninsert link");
            channel.sendMessage(eb.build()).queue();
            return;
        }
        args.remove(0);
        Language originalLanguage = Language.fromString(TranslateUtils.detect(SecretConstants.YAND_TRAN_SECRET, String.join(" ", args)));
        String translated = TranslateUtils.translate(SecretConstants.YAND_TRAN_SECRET, String.join(" ", args), originalLanguage, finalLanguage);
        EmbedBuilder eb = new EmbedBuilder();
        eb
                .setColor(Constants.VESTRIMU_PURPLE)
                .setTitle("**Success**")
                .addField("Translation", translated, false);
        channel.sendMessage(eb.build()).queue();
    }

}