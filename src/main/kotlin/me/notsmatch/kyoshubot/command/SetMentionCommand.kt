package me.notsmatch.kyoshubot.command

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import me.notsmatch.kyoshubot.service.BoshuService
import me.notsmatch.kyoshubot.service.GuildSettingsService
import me.notsmatch.kyoshubot.util.NumberUtils
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import org.apache.commons.lang3.StringUtils
import java.awt.Color

class SetMentionCommand(val boshuService: BoshuService, val settingsService: GuildSettingsService) : Command() {

    init {
        this.name = "setmention"
        this.help = "募集時のメンションを変更します (noneで非表示)"
        this.arguments = "<role_id | none | everyone | here>"
    }

    override fun execute(event: CommandEvent?) {
        event?.apply {

            val settings = settingsService.getGuildSettings(guild.idLong)
            if(settings.banned)return reply("This server has been banned.")
            if (settings.getCommandOption("setmention") == null || !settings.getCommandOption("setmention")!!.visibility) {
                event.message.delete().complete()
            }

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

            val args = StringUtils.split(args)
            if(args.isEmpty()){
                return replyInDm(EmbedBuilder().apply {
                    setColor(Color.RED)
                    setAuthor(
                        "Error",
                        null,
                        null
                    )
                    setDescription("``.setmention <role_id | none | everyone | here>``")
                }.build())
            }

            if(args[0].equals("everyone", true)){
                settings.apply {
                    mention = "everyone"
                    save()
                    val boshuList = boshuService.getBoshuListByGuildId(guild.idLong) ?: return
                    boshuList.forEach { boshu ->
                        boshu.updateMessage(guild, settings, false)
                    }
                }
                return replyInDm(EmbedBuilder().apply {
                    setColor(Color.CYAN)
                    setAuthor(
                        "メンションを変更しました",
                        null,
                        null
                    )
                    setDescription("メンションはeveryoneになりました")
                }.build())
            }

            else if(args[0].equals("here", true)){
                settings.apply {
                    mention = "here"
                    save()
                    val boshuList = boshuService.getBoshuListByGuildId(guild.idLong) ?: return
                    boshuList.forEach { boshu ->
                        boshu.updateMessage(guild, settings, false)
                    }
                }
                return replyInDm(EmbedBuilder().apply {
                    setColor(Color.CYAN)
                    setAuthor(
                        "メンションを変更しました",
                        null,
                        null
                    )
                    setDescription("メンションはhereになりました")
                }.build())
            }

            else if(args[0].equals("none", true)){
                settings.apply {
                    mention = "none"
                    save()
                    val boshuList = boshuService.getBoshuListByGuildId(guild.idLong) ?: return
                    boshuList.forEach { boshu ->
                        boshu.updateMessage(guild, settings, false)
                    }
                }
                return replyInDm(EmbedBuilder().apply {
                    setColor(Color.CYAN)
                    setAuthor(
                        "メンションを変更しました",
                        null,
                        null
                    )
                    setDescription("メンションは非表示になりました")
                }.build())
            }

            else if(!NumberUtils.isLong(args[0])){
                return replyInDm(EmbedBuilder().apply {
                    setColor(Color.RED)
                    setAuthor(
                        "Error",
                        null,
                        null
                    )
                    setDescription("``.setmention <role_id | none | everyone | here>``")
                }.build())
            } else {

                val roleId = args[0].toLong()

                val role = jda.getRoleById(roleId)
                    ?: return replyInDm(EmbedBuilder().apply {
                        setColor(Color.RED)
                        setAuthor(
                            "Error",
                            null,
                            null
                        )
                        setDescription("そのサーバーには存在しないRoleです")
                    }.build())

                if(!role.isMentionable){
                    return replyInDm(EmbedBuilder().apply {
                        setColor(Color.RED)
                        setAuthor(
                            "Error",
                            null,
                            null
                        )
                        setDescription("ロールのメンションを許可してください")
                    }.build())
                }

                settings.apply {
                    mention = args[0]
                    save()
                    val boshuList = boshuService.getBoshuListByGuildId(guild.idLong) ?: return
                    boshuList.forEach { boshu ->
                        boshu.updateMessage(guild, settings, false)
                    }
                }
            }
        }
    }
}