package me.notsmatch.kyoshubot.command

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import me.notsmatch.kyoshubot.model.KyoshuUser
import me.notsmatch.kyoshubot.service.BoshuService
import me.notsmatch.kyoshubot.service.GuildSettingsService
import me.notsmatch.kyoshubot.util.NumberUtils
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import org.apache.commons.lang3.StringUtils
import java.awt.Color

class CanCommand(val boshuService: BoshuService, val settingsService: GuildSettingsService) : Command(){

    init {
        this.name = "c"
        this.help = "時間を指定して挙手します | 管理者専用: .c <hour> <mention>"
        this.arguments = "<hour1> <hour2> <hour3>..."
    }

    override fun execute(event: CommandEvent?) {
        event?.apply {

            val settings = settingsService.getGuildSettings(guild.idLong)
            if(settings.banned)return reply("This server has been banned.")
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

                if(args.size == 2 && args[1].startsWith("<@") && args[1].endsWith('>')) {
                    val role = guild.getRolesByName("Kyoshu Admin", true).first()
                    if (!member.hasPermission(Permission.ADMINISTRATOR) && role == null || !member.hasPermission(Permission.ADMINISTRATOR) &&  !member.roles.contains(role)) {
                        return replyInDm(EmbedBuilder().apply {
                            setColor(Color.RED)
                            setAuthor(
                                "Error",
                                null,
                                null
                            )
                            setDescription("管理者権限　または　権限ロール(Kyoshu Admin)が必要です。")
                        }.build())
                    }

                    var mention = args[1].replace("<", "").replace("@", "").replace(">", "").trim()

                    if (mention.startsWith('!')) {
                        mention = mention.replace("!", "").trim()
                    }

                    val other = guild.getMemberById(mention) ?: return replyInDm(EmbedBuilder().apply {
                        setColor(Color.RED)
                        setAuthor(
                            "Error",
                            null,
                            null
                        )
                        setDescription("そのユーザーはサーバー内に存在しません")
                    }.build())

                    if (!NumberUtils.isInteger(args[0]) || args[0].toInt() > 36 || args[0].toInt() < 0) {
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

                    //項目が存在しない場合
                    val koumoku = boshu.getKoumokuByHour(args[0].toInt()) ?: return replyInDm(EmbedBuilder().apply {
                        setColor(Color.RED)
                        setAuthor(
                            "Error",
                            null,
                            null
                        )
                        setDescription("${args[0]}時の項目は存在しません")
                    }.build())

                    //既に仮挙手している場合、挙手にする
                    val user = koumoku.getKyoshuUser(other.idLong)
                    if (user != null && koumoku.getKyoshuUser(other.idLong)!!.temporary) {
                        user.temporary = false
                        boshu.save()

                        boshu.updateMessage(guild, settings, false)
                        return
                    }

                    //締め切られている場合
                    else if (koumoku.isClosed()) {
                        return replyInDm(EmbedBuilder().apply {
                            setColor(Color.RED)
                            setAuthor(
                                "Error",
                                null,
                                null
                            )
                            setDescription("${args[0]}時の項目は締め切られています")
                        }.build())
                    }

                    //既に本挙手している場合、エラー
                    else if (user != null && !koumoku.getKyoshuUser(other.idLong)!!.temporary) {
                        return replyInDm(EmbedBuilder().apply {
                            setColor(Color.RED)
                            setAuthor(
                                "Error",
                                null,
                                null
                            )
                            setDescription("${other.effectiveName}は既に${args[0]}時に挙手しています")
                        }.build())
                    }

                    //本挙手していない場合
                    else if (!koumoku.isKyoshu(other.idLong)) {
                        if (koumoku.kyoshuUsers.add(KyoshuUser(other.idLong, false))) {

                            boshu.save()

                            boshu.updateMessage(guild, settings, false)
                        }
                    }
                    return
                }

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

                    //項目が存在しない場合
                    val koumoku = boshu.getKoumokuByHour(arg.toInt()) ?: return replyInDm(EmbedBuilder().apply {
                        setColor(Color.RED)
                        setAuthor(
                            "Error",
                            null,
                            null
                        )
                        setDescription("${arg}時の項目は存在しません")
                    }.build())

                    //既に締め切られている場合
                     if (koumoku.isClosed()) {
                    return replyInDm(EmbedBuilder().apply {
                        setColor(Color.RED)
                        setAuthor(
                            "Error",
                            null,
                            null
                        )
                        setDescription("${arg}時の項目は締め切られています")
                    }.build())
                }

                    //既に仮挙手している場合、本挙手にする
                    val user = koumoku.getKyoshuUser(author.idLong)
                    if (user != null && koumoku.getKyoshuUser(author.idLong)!!.temporary) {
                        user.temporary = false
                        boshu.save()

                        boshu.updateMessage(guild, settings, false)
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

                            boshu.updateMessage(guild, settings, false)
                        }
                    }
                }
            }
        }
    }
}