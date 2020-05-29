package me.notsmatch.kyoshubot.command

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import me.notsmatch.kyoshubot.model.KyoshuUser
import me.notsmatch.kyoshubot.service.BoshuService
import me.notsmatch.kyoshubot.service.GuildSettingsService
import me.notsmatch.kyoshubot.util.DiscordUtils
import me.notsmatch.kyoshubot.util.NumberUtils
import net.dv8tion.jda.api.EmbedBuilder
import org.apache.commons.lang3.StringUtils
import java.awt.Color
import java.lang.StringBuilder

class CanCommand(val boshuService: BoshuService, val settingsService: GuildSettingsService) : Command(){

    init {
        this.name = "c"
        this.help = "時間を指定して挙手します"
        this.arguments = "<hour1> <hour2> <hour3>..."
    }

    override fun execute(event: CommandEvent?) {
        event?.apply {
            val settings = settingsService.getGuildSettings(guild.idLong)

            if (settings.getCommandOption("c") == null || !settings.getCommandOption("c")!!.visibility) {
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

            if (args.isNotEmpty()) {

                args.forEach { arg ->
                    if (!NumberUtils.isInteger(arg) || arg.toInt() > 36 || arg.toInt() < 0) {
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

                    val koumoku = boshu.getKoumokuByHour(arg.toInt()) ?: return replyInDm(EmbedBuilder().apply {
                        setColor(Color.RED)
                        setAuthor(
                            "Error",
                            null,
                            null
                        )
                        setDescription("${arg}時の項目は存在しません")
                    }.build())

                    if (koumoku.getKyoshuSize() >= koumoku.need) {
                        return replyInDm(EmbedBuilder().apply {
                            setColor(Color.RED)
                            setAuthor(
                                "Error",
                                null,
                                null
                            )
                            setDescription("${arg}時の項目は挙手人数が満員に達しています")
                        }.build())
                    }

                    //既に仮挙手している場合、挙手にする
                    val user = koumoku.getKyoshuUser(author.idLong)
                    if (user != null && koumoku.getKyoshuUser(author.idLong)!!.temporary) {
                        user.temporary = false
                        boshu.save()

                        boshu.updateMessage(guild, settings)
                        return
                    }

                    //既に本挙手している場合、エラー
                    else if (user != null && !koumoku.getKyoshuUser(author.idLong)!!.temporary) {
                        return replyInDm(EmbedBuilder().apply {
                            setColor(Color.RED)
                            setAuthor(
                                "Error",
                                null,
                                null
                            )
                            setDescription("あなたは既に${arg}時に挙手しています")
                        }.build())
                    }

                    //本挙手していない場合
                   else if (!koumoku.isKyoshu(author.idLong)) {
                        if (koumoku.kyoshuUsers.add(KyoshuUser(author.idLong, false))) {

                            boshu.save()

                            boshu.updateMessage(guild, settings)
                        }
                    }
                }
            }
        }
    }
}