package me.notsmatch.kyoshubot.command

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import me.notsmatch.kyoshubot.model.Koumoku
import me.notsmatch.kyoshubot.service.BoshuService
import me.notsmatch.kyoshubot.service.GuildSettingsService

import me.notsmatch.kyoshubot.util.NumberUtils
import net.dv8tion.jda.api.EmbedBuilder
import org.apache.commons.lang3.StringUtils
import java.awt.Color
import java.lang.StringBuilder
import java.util.concurrent.TimeUnit

class AddCommand(val boshuService: BoshuService,  val settingsService: GuildSettingsService) : Command(){

    init {
        this.name = "add"
        this.help = "項目を追加します"
        this.arguments = "<hour> <need> <title> "
    }

    override fun execute(event: CommandEvent?) {
        event?.apply {
            val settings = settingsService.getGuildSettings(guild.idLong)

            if(settings.banned)return reply("This server has been banned.")

            if (settings.getCommandOption("add") == null || !settings.getCommandOption("add")!!.visibility) {
                event.message.delete().complete()
            }

            val boshu = boshuService.getBoshu(guild.idLong, channel.idLong)
                ?: return replyInDm(EmbedBuilder().apply {
                    setColor(Color.RED)
                    setAuthor(
                        "Error",
                        null,
                        null
                    )
                    setDescription("このチャンネルでは募集が開始されていません。")
                }.build())

            val args = StringUtils.split(args)
            var title: String = ""
            var hour: String = ""
            var need: String = ""

            if(args.size >= 2) {
                hour = args[0]
                need = args[1]

                if(args.size >= 3) {
                    val b = StringBuilder()

                    val it = args.slice(IntRange(2, args.size - 1)).iterator()
                    while (it.hasNext()) {
                        val next = it.next()
                        b.append(next)

                        if (it.hasNext()) b.append(" ")
                    }

                    title = b.toString()
                }

            } else {
                return replyInDm(EmbedBuilder().apply {
                    setColor(Color.RED)
                    setAuthor(
                        "Error",
                        null,
                        null
                    )
                    setDescription(".add <title> <hour> <need>")
                }.build())
            }


            if(boshu.getKoumokuByHour(hour.toInt()) != null) {
                return replyInDm(EmbedBuilder().apply {
                    setColor(Color.RED)
                    setAuthor(
                        "Error",
                        null,
                        null
                    )
                    setDescription("${hour}時は既に項目が存在します")
                }.build())
            }

            if(title.length > 30){
                return replyInDm(EmbedBuilder().apply {
                    setColor(Color.RED)
                    setAuthor(
                        "Error",
                        null,
                        null
                    )
                    setDescription("titleは30文字以下にする必要があります。")
                }.build())
            }
            else if(!NumberUtils.isInteger(hour) || hour.toInt() > 36 || hour.toInt() < 0){
                return replyInDm(EmbedBuilder().apply {
                    setColor(Color.RED)
                    setAuthor(
                        "Error",
                        null,
                        null
                    )
                    setDescription("hourは0~36で指定する必要があります。")
                }.build())
            }
            else if(!NumberUtils.isInteger(need) || need.toInt() > 30 || need.toInt() < 0){
                return replyInDm(EmbedBuilder().apply {
                    setColor(Color.RED)
                    setAuthor(
                        "Error",
                        null,
                        null
                    )
                    setDescription("needは30名まで指定可能です")
                }.build())
            }

            //項目追加
            if(boshu.koumokuList.add(
                    Koumoku(
                        title,
                        hour.toInt(),
                        need.toInt(),
                        false,
                        mutableListOf()
                    )
                )){

                boshu.koumokuList = boshu.koumokuList.sortedWith(kotlin.Comparator { o1, o2 -> if (o1.hour > o2.hour) 1 else -1; }).toMutableList()

                boshu.save()

                boshu.updateMessage(guild, settings, false)
            }
        }
    }
}