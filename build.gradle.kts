defaultTasks(
        "clean",
        "buildOS5",
        "zip"
)

var bb_buildfile = listOf<String>(
        "**/*.cod",
        "**/*.debug",
        "**/*.jad",
        "**/*.jar",
        "**/*.export.xml",
        "**/*.csl",
        "**/*.cso"
)
var folder = project.projectDir
var api5_path = "C:\\Program Files (x86)\\Research In Motion\\BlackBerry JDE 5.0.0"
var api7_path = "C:\\Program Files (x86)\\Research In Motion\\BlackBerry JDE 7.1.0"
var jdk_path = "C:\\Program Files (x86)\\Java\\jdk1.5.0_22\\bin"

var warnkeyRelease = "warnkey=0x52424200;0x52525400;0x5242534b;0x42424944;0x52435200;0x4e464352;0x52455345"
var warnkey = "warnkey=0x52424200;0x52525400;0x52435200"

task("buildOS5") {
    doLast {
        delete("build")
        exec {
            commandLine = listOf(
                    "${api5_path}\\bin\\rapc.exe",
                    "-quiet",
                    "library=build\\OS5\\Mail",
                    "Mail\\Mail.rapc",
                    warnkeyRelease,
                    "@Mail_build.files"
            )
        }
        exec {
            commandLine = listOf(
                    "${api5_path}\\bin\\rapc.exe",
                    "-quiet",
                    "library=build\\OS5\\MailOS46",
                    "MailOS46\\MailOS46.rapc",
                    warnkeyRelease,
                    "import=build\\OS5\\Mail.jar;${api5_path}\\lib\\net_rim_api.jar",
                    "${folder}\\MailOS46\\src\\org\\logicprobe\\LogicMail\\PlatformInfoBB46.java",
                    "${folder}\\MailOS46\\src\\org\\logicprobe\\LogicMail\\ui\\FieldFactoryBB46.java",
                    "${folder}\\MailOS46\\src\\org\\logicprobe\\LogicMail\\ui\\NotificationHandlerBB46.java",
                    "${folder}\\MailOS46\\src\\org\\logicprobe\\LogicMail\\util\\UtilFactoryBB46.java"
            )
        }

        exec {
            commandLine = listOf(
                    "${api5_path}\\bin\\rapc.exe",
                    "-quiet",
                    "library=build\\OS5\\MailOS47",
                    "MailOS47\\MailOS47.rapc",
                    warnkeyRelease,
                    "import=build\\OS5\\Mail.jar;build\\OS5\\MailOS46.jar;${api5_path}\\lib\\net_rim_api.jar",
                    "${folder}\\MailOS47\\src\\org\\logicprobe\\LogicMail\\PlatformInfoBB47.java",
                    "${folder}\\MailOS47\\src\\org\\logicprobe\\LogicMail\\ui\\FieldFactoryBB47.java",
                    "${folder}\\MailOS47\\src\\org\\logicprobe\\LogicMail\\ui\\ScreenFactoryBB47.java",
                    "${folder}\\MailOS47\\src\\org\\logicprobe\\LogicMail\\ui\\ShortcutBarButtonField.java",
                    "${folder}\\MailOS47\\src\\org\\logicprobe\\LogicMail\\ui\\ShortcutBarManager.java",
                    "${folder}\\MailOS47\\src\\org\\logicprobe\\LogicMail\\ui\\StandardTouchScreen.java",
                    "${folder}\\MailOS47\\src\\org\\logicprobe\\LogicMail\\ui\\TouchMailHomeScreen.java",
                    "${folder}\\MailOS47\\src\\org\\logicprobe\\LogicMail\\ui\\TouchNodeIcons.java",
                    "${folder}\\MailOS47\\src\\org\\logicprobe\\LogicMail\\ui\\TouchScreenTreeField.java"
            )
        }

        exec {
            commandLine = listOf(
                    "${api5_path}\\bin\\rapc.exe",
                    "-quiet",
                    "library=build\\OS5\\MailOS5",
                    "MailOS5\\MailOS5.rapc",
                    warnkeyRelease,
                    "import=build\\OS5\\Mail.jar;build\\OS5\\MailOS46.jar;build\\OS5\\MailOS47.jar;${api5_path}\\lib\\net_rim_api.jar",
                    "${folder}\\MailOS5\\src\\org\\logicprobe\\LogicMail\\PlatformInfoBB50.java",
                    "${folder}\\MailOS5\\src\\org\\logicprobe\\LogicMail\\ui\\FieldFactoryBB50.java",
                    "${folder}\\MailOS5\\src\\org\\logicprobe\\LogicMail\\ui\\ScreenFactoryBB50.java",
                    "${folder}\\MailOS5\\src\\org\\logicprobe\\LogicMail\\util\\NetworkConnectorBB50.java",
                    "${folder}\\MailOS5\\src\\org\\logicprobe\\LogicMail\\util\\UtilFactoryBB50.java"
            )
        }

        exec {
            commandLine = listOf(
                    "${api5_path}\\bin\\rapc.exe",
                    "-quiet",
                    "codename=MailStartup\\EMail",
                    "MailStartup\\EMail.rapc",
                    warnkeyRelease,
                    "import=build\\OS5\\Mail.jar;build\\OS5\\MailOS46.jar;build\\OS5\\MailOS47.jar;build\\OS5\\MailOS5.jar;${api5_path}\\lib\\net_rim_api.jar",
                    "${folder}\\MailStartup\\res\\icons\\messages.png",
                    "${folder}\\MailStartup\\res\\icons\\messages_roll.png",
                    "${folder}\\MailStartup\\src\\org\\logicprobe\\LogicMail\\MailStartup.java"
            )
        }
    }
}

tasks.create("signSource") {
    doLast {
        exec {
            commandLine("${jdk_path}\\javaw.exe",
                    "-jar",
                    "${api7_path}\\bin\\SignatureTool.jar",
                    "-r",
                    "${folder}/build/OS5"
            )
            workingDir(api7_path)
        }
        delete("${folder}/build/OS5/cache")
    }
}

tasks.create<Copy>("copy") {
    dependsOn(tasks.getByName("signSource"))
    from("build/OS5")
    into("build/OS5/cache")
    include("*.cod", "*.jad")
}

tasks.register<Zip>("zip") {
    dependsOn(tasks.getByName("copy"))
    var jdp_text = File("MailStartup/MailStartup.jdp").readText(charset("utf-8"))
    var version = jdp_text.split("Version=")[1].trim()
    var DependsOn = jdp_text.split("[DependsOn\r\n")[1].trim().split("\r\n")[0]
    archiveFileName.set("${DependsOn}-${version}.zip")
    destinationDirectory.set(layout.projectDirectory.dir("build"))
    from("${folder}/build/OS5/cache")
}

task("clean") {
    doLast {
        delete("build/OS5")
        delete("build/OS6")
        delete(fileTree("Mail").matching {
            include(bb_buildfile)
        })
        delete(fileTree("MailOS5").matching {
            include(bb_buildfile)
        })
        delete(fileTree("MailOS6").matching {
            include(bb_buildfile)
        })
        delete(fileTree("MailOS46").matching {
            include(bb_buildfile)
        })
        delete(fileTree("MailOS47").matching {
            include(bb_buildfile)
        })
        delete(fileTree("MailStartup").matching {
            include(bb_buildfile)
        })
        delete("dist")
    }
}

