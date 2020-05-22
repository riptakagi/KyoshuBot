package me.notsmatch.kyoshubot.command

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import me.notsmatch.kyoshubot.service.BoshuService
import me.notsmatch.kyoshubot.service.MentionService
import me.notsmatch.kyoshubot.util.DiscordUtils
import me.notsmatch.kyoshubot.util.NumberUtils
import net.dv8tion.jda.api.EmbedBuilder
import org.apache.commons.lang3.StringUtils
import java.awt.Color
import java.lang.StringBuilder

class RemoveCommand(val boshuService: BoshuService, val mentionService: MentionService) : Command(){

    init {
        this.name = "remove"
        this.help = "項目を削除します"
        this.arguments = "<hour>"
    }

    override fun execute(event: CommandEvent?) {
        event?.apply {

            event.message.delete().complete()

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

            var hour: String = ""
            if (args.size >= 1) {
                hour = args[0]
            }

            if(!NumberUtils.isInteger(hour) || hour.toInt() > 24 || hour.toInt() < 0){
                return replyInDm(EmbedBuilder().apply {
                    setColor(Color.RED)
                    setAuthor(
                        "Error",
                        null,
                        null
                    )
                    setDescription("hourは0~24で指定する必要があります。")
                }.build())
            }

            val koumoku = boshu.getKoumokuByHour(hour.toInt()) ?:
            return replyInDm(EmbedBuilder().apply {
                setColor(Color.RED)
                setAuthor(
                    "Error",
                    null,
                    null
                )
                setDescription("${hour}時の項目は存在しません")
            }.build())

            if(boshu.koumokuList.remove(koumoku)){

                boshu.save()

                replyInDm(
                    EmbedBuilder().apply {
                        setColor(Color.CYAN)
                        setAuthor(
                            "${hour}時の項目を削除しました",
                            null,
                            null
                        )
                    }.build()
                )
                textChannel.editMessageById(boshu.messageId,  EmbedBuilder().apply {
                    setColor(Color.CYAN)
                    setAuthor(
                        "募集が進行中です",
                        null,
                        null
                    )
                    val builder = StringBuilder("${mentionService.getMentionByGuild(guild)}\nタイトル: " + boshu.title + "\n" + ".add <hour> <need> <title> を使用して挙手項目を追加してください。")
                    builder.append("==========================\n")
                    val it = boshu.koumokuList.iterator()
                    while (it.hasNext()) {
                        val k = it.next()
                        val b = StringBuilder("・${k.hour}時 ${k.kyoshuSizeText()} ${k.title}")
                        if (k.kyoshuUsers.size >= 1) {
                            b.append("\n")
                            k.kyoshuUsers.forEach { id ->
                                val member = guild.getMemberById(id)
                                if (member != null) {
                                    b.append(DiscordUtils.getName(member) + " ")
                                }
                            }
                        }
                        builder.append(b.toString())
                        if (it.hasNext()) {
                            builder.append("\n")
                        }
                    }
                    setDescription(builder.toString())
                }.build()).queue()
            }
        }
    }
}