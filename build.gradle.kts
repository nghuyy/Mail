defaultTasks(
        "clean",
        "setupVersion",
        "setupOS5",
        "buildOS5",
        "zip",
        "setupOS6",
        "buildOS6",
        "zip6",
        "Release"
)
var projectName = "Mail"
var buildNumber = 20
var localVersion = "1.0.${buildNumber}"


val manifest = file("./manifest.json").takeIf { it.exists() }?.let {
    groovy.json.JsonSlurper().parseText(it.readText())
} as Map<*, *>?
if (manifest != null) {
    val json = manifest.toMutableMap()
    buildNumber = (json.get("build") as Int) + 1
    localVersion = "1.0.${buildNumber}"
}
var buildtime = java.text.SimpleDateFormat("hh:mm aa dd/MM/yyyy").format(java.util.Date())

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
var api6_path = "C:\\Program Files (x86)\\Research In Motion\\BlackBerry JDE 6.0.0"
var api7_path = "C:\\Program Files (x86)\\Research In Motion\\BlackBerry JDE 7.1.0"
var jdk_path = "C:\\Program Files (x86)\\Java\\jdk1.5.0_22\\bin"
var warnkeyRelease = "warnkey=0x52424200;0x52525400;0x5242534b;0x42424944;0x52435200;0x4e464352;0x52455345"
var warnkey = "warnkey=0x52424200;0x52525400;0x52435200"
var packID = "blackberry.sig"
var passwordPath = rootProject.file(System.getProperty("user.home") + "/.gradle/.keystore").readText(charset("utf-8"))
var password = rootProject.file("${passwordPath}\\${packID}").readText(charset("utf-8"))

task("testPass"){
    doLast{
        println(password)
    }
}

task("Build"){
    doLast{
        SetupVersion()
        GenerateFiles(true)
        GenerateFiles(false)
        build(false)
        build(true)
    }
}

fun SetupVersion() {
    //setup files
    if (manifest != null) {
        val json = manifest.toMutableMap()
        buildNumber = (json.get("build") as Int) + 1
        localVersion = "1.0.${buildNumber}"
    }
    var appversion = File("Mail/${projectName}_OS5.jdp").readText(charset("utf-8")).split("Version=")[1].trim()
    File("Mail/${projectName}_OS5.jdp").writeText(
            File("Mail/${projectName}_OS5.jdp").readText(charset("utf-8"))
                    .replace("Version=${appversion}", "Version=${localVersion}"), charset("utf-8")
    )
    File("Mail/${projectName}_OS5.rapc").writeText(
            File("Mail/${projectName}_OS5.rapc").readText(charset("utf-8"))
                    .replace("MIDlet-Version: ${appversion}", "MIDlet-Version: ${localVersion}"), charset("utf-8")
    )

    File("Mail/${projectName}_OS6.jdp").writeText(
            File("Mail/${projectName}_OS6.jdp").readText(charset("utf-8"))
                    .replace("Version=${appversion}", "Version=${localVersion}"), charset("utf-8")
    )
    File("Mail/${projectName}_OS6.rapc").writeText(
            File("Mail/${projectName}_OS6.rapc").readText(charset("utf-8"))
                    .replace("MIDlet-Version: ${appversion}", "MIDlet-Version: ${localVersion}"), charset("utf-8")
    )

    var appversion1 = File("MailStartup/MailStartup.jdp").readText(charset("utf-8")).split("Version=")[1].trim()
    File("MailStartup/MailStartup.jdp").writeText(
            File("MailStartup/MailStartup.jdp").readText(charset("utf-8"))
                    .replace("Version=${appversion1}", "Version=${localVersion}"), charset("utf-8")
    )
    File("MailStartup/MailStartup.rapc").writeText(
            File("MailStartup/MailStartup.rapc").readText(charset("utf-8"))
                    .replace("MIDlet-Version: ${appversion1}", "MIDlet-Version: ${localVersion}"), charset("utf-8")
    )

    val releaseNote = mutableMapOf<Any, Any>()
    releaseNote.run {
        put("build", buildNumber)
        put("version", localVersion)
        put("app", projectName)
        put("time", buildtime)
        releaseNote
    }
    File("./manifest.json").writeText(
            groovy.json.JsonBuilder(releaseNote).toPrettyString(),
            java.nio.charset.Charset.forName("utf-8")
    )
}

fun GenerateFiles(v6:Boolean) {
    var filesStr = if(v6)"import=${api6_path}\\lib\\net_rim_api.jar\n" else "import=${api5_path}\\lib\\net_rim_api.jar\n"
    var OSString = if(v6)"OS6" else "OS5"
    var shortPathStr = ""
    project.fileTree("Mail\\src").filter { it.isFile() }.files.forEach {
        filesStr += it.path + "\n"
        shortPathStr += it.path.replace(project.projectDir.toString() + "\\Mail\\", "") + "\n"
    }
    project.fileTree("Mail\\res").filter { it.isFile() }.files.forEach {
        filesStr += it.path + "\n"
        shortPathStr += it.path.replace(project.projectDir.toString() + "\\Mail\\", "") + "\n"
    }
    File("Mail\\${projectName}_${OSString}.files").writeText(filesStr, charset("utf-8"))
    var jdp_str = File("Mail\\${projectName}_${OSString}.jdp").readText(charset("utf-8"))
    var arraystr = jdp_str.split("[Files\r\n")
    var firsttmp = arraystr[0]
    var lasttmp = arraystr[1]
    var laststr = lasttmp.split(Regex("\\]"), 2)[1]
    File("Mail\\${projectName}_${OSString}.jdp").writeText(firsttmp + "[Files\r\n" + shortPathStr + "]" + laststr, charset("utf-8"))
}

fun build(osv6:Boolean){
    exec {
        commandLine = listOf(
                if (osv6) "${api6_path}\\bin\\rapc.exe" else "${api5_path}\\bin\\rapc.exe",
                "-quiet",
                if (osv6) "library=build\\OS6\\Mail" else "library=build\\OS5\\Mail",
                if (osv6) "Mail\\Mail_OS6.rapc" else "Mail\\Mail_OS5.rapc",
                warnkeyRelease,
                if (osv6) "@Mail\\Mail_OS6.files" else "@Mail\\Mail_OS5.files"
        )
    }
    exec {
        commandLine = listOf(
                if (osv6) "${api6_path}\\bin\\rapc.exe" else "${api5_path}\\bin\\rapc.exe",
                "-quiet",
                if (osv6) "library=build\\OS6\\MailOS46" else "library=build\\OS5\\MailOS46",
                "MailOS46\\MailOS46.rapc",
                warnkeyRelease,
                 if (osv6) "import=build\\OS6\\Mail.jar;${api6_path}\\lib\\net_rim_api.jar" else "import=build\\OS5\\Mail.jar;${api5_path}\\lib\\net_rim_api.jar",
                "${folder}\\MailOS46\\src\\org\\logicprobe\\LogicMail\\PlatformInfoBB46.java",
                "${folder}\\MailOS46\\src\\org\\logicprobe\\LogicMail\\ui\\FieldFactoryBB46.java",
                "${folder}\\MailOS46\\src\\org\\logicprobe\\LogicMail\\ui\\NotificationHandlerBB46.java",
                "${folder}\\MailOS46\\src\\org\\logicprobe\\LogicMail\\util\\UtilFactoryBB46.java"
        )
    }

    exec {
        commandLine = listOf(
                if (osv6) "${api6_path}\\bin\\rapc.exe" else "${api5_path}\\bin\\rapc.exe",
                "-quiet",
                if (osv6) "library=build\\OS6\\MailOS47" else "library=build\\OS5\\MailOS47",
                "MailOS47\\MailOS47.rapc",
                warnkeyRelease,
                if (osv6) "import=build\\OS6\\Mail.jar;build\\OS6\\MailOS46.jar;${api6_path}\\lib\\net_rim_api.jar" else "import=build\\OS5\\Mail.jar;build\\OS5\\MailOS46.jar;${api5_path}\\lib\\net_rim_api.jar",
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
                if (osv6) "${api6_path}\\bin\\rapc.exe" else "${api5_path}\\bin\\rapc.exe",
                "-quiet",
                if (osv6) "library=build\\OS6\\MailOS5" else "library=build\\OS5\\MailOS5",
                "MailOS5\\MailOS5.rapc",
                warnkeyRelease,
                if (osv6) "import=build\\OS6\\Mail.jar;build\\OS6\\MailOS46.jar;build\\OS6\\MailOS47.jar;${api6_path}\\lib\\net_rim_api.jar" else "import=build\\OS5\\Mail.jar;build\\OS5\\MailOS46.jar;build\\OS5\\MailOS47.jar;${api5_path}\\lib\\net_rim_api.jar",
                "${folder}\\MailOS5\\src\\org\\logicprobe\\LogicMail\\PlatformInfoBB50.java",
                "${folder}\\MailOS5\\src\\org\\logicprobe\\LogicMail\\ui\\FieldFactoryBB50.java",
                "${folder}\\MailOS5\\src\\org\\logicprobe\\LogicMail\\ui\\ScreenFactoryBB50.java",
                "${folder}\\MailOS5\\src\\org\\logicprobe\\LogicMail\\util\\NetworkConnectorBB50.java",
                "${folder}\\MailOS5\\src\\org\\logicprobe\\LogicMail\\util\\UtilFactoryBB50.java"
        )
    }
    if(!osv6){
        exec {
            commandLine = listOf(
                    "${api5_path}\\bin\\rapc.exe",
                    "-quiet",
                    "codename=build\\OS5\\EMail",
                    "MailStartup\\MailStartup.rapc",
                    warnkeyRelease,
                    "import=build\\OS5\\Mail.jar;build\\OS5\\MailOS46.jar;build\\OS5\\MailOS47.jar;build\\OS5\\MailOS5.jar;${api5_path}\\lib\\net_rim_api.jar",
                    "${folder}\\MailStartup\\res\\icons\\messages.png",
                    "${folder}\\MailStartup\\res\\icons\\messages_roll.png",
                    "${folder}\\MailStartup\\src\\org\\logicprobe\\LogicMail\\MailStartup.java"
            )
        }
    }else {
        exec {
            commandLine = listOf(
                    if (osv6) "${api6_path}\\bin\\rapc.exe" else "${api5_path}\\bin\\rapc.exe",
                    "-quiet",
                    if (osv6) "library=build\\OS6\\MailOS6" else "library=build\\OS5\\MailOS6",
                    "MailOS6\\MailOS6.rapc",
                    warnkeyRelease,
                    if (osv6) "import=build\\OS6\\Mail.jar;build\\OS6\\MailOS46.jar;build\\OS6\\MailOS47.jar;build\\OS6\\MailOS5.jar;${api6_path}\\lib\\net_rim_api.jar" else "import=build\\OS5\\Mail.jar;build\\OS5\\MailOS46.jar;build\\OS5\\MailOS47.jar;build\\OS5\\MailOS5.jar;${api5_path}\\lib\\net_rim_api.jar",
                    "${folder}\\MailOS6\\res\\icons\\go-bottom_32x32.png",
                    "${folder}\\MailOS6\\res\\icons\\go-next_32x32.png",
                    "${folder}\\MailOS6\\res\\icons\\go-previous_32x32.png",
                    "${folder}\\MailOS6\\res\\icons\\go-top_32x32.png",
                    "${folder}\\MailOS6\\res\\icons\\message-mark-opened_32x32.png",
                    "${folder}\\MailOS6\\res\\icons\\message-mark-unopened_32x32.png",
                    "${folder}\\MailOS6\\res\\icons\\message-reply-all_32x32.png",
                    "${folder}\\MailOS6\\res\\icons\\message-undelete_32x32.png",
                    "${folder}\\MailOS6\\res\\icons\\messages.png",
                    "${folder}\\MailOS6\\res\\icons\\messages_roll.png",
                    "${folder}\\MailOS6\\res\\icons\\search_32x32.png",
                    "${folder}\\MailOS6\\src\\org\\logicprobe\\LogicMail\\PlatformInfoBB60.java",
                    "${folder}\\MailOS6\\src\\org\\logicprobe\\LogicMail\\util\\UtilFactoryBB60.java",
                    "${folder}\\MailOS6\\src\\org\\logicprobe\\LogicMail\\ui\\BrowserField2Renderer.java",
                    "${folder}\\MailOS6\\src\\org\\logicprobe\\LogicMail\\ui\\FieldFactoryBB60.java",
                    "${folder}\\MailOS6\\src\\org\\logicprobe\\LogicMail\\ui\\MailboxScreenBB60.java",
                    "${folder}\\MailOS6\\src\\org\\logicprobe\\LogicMail\\ui\\MessageActionsBB60.java",
                    "${folder}\\MailOS6\\src\\org\\logicprobe\\LogicMail\\ui\\NotificationHandlerBB60.java",
                    "${folder}\\MailOS6\\src\\org\\logicprobe\\LogicMail\\ui\\ScreenFactoryBB60.java",
                    "${folder}\\MailOS6\\src\\org\\logicprobe\\LogicMail\\ui\\StandardScreenBB60.java",
                    "${folder}\\MailOS6\\src\\org\\logicprobe\\LogicMail\\ui\\StandardTouchScreenBB60.java"
            )
        }

        exec {
            commandLine = listOf(
                    if (osv6) "${api6_path}\\bin\\rapc.exe" else "${api5_path}\\bin\\rapc.exe",
                    "-quiet",
                    if (osv6) "codename=build\\OS6\\EMail" else "codename=build\\OS5\\EMail",
                    "MailStartup\\MailStartup.rapc",
                    warnkeyRelease,
                    if (osv6) "import=build\\OS6\\Mail.jar;build\\OS6\\MailOS46.jar;build\\OS6\\MailOS47.jar;build\\OS6\\MailOS6.jar;build\\OS6\\MailOS6.jar;${api6_path}\\lib\\net_rim_api.jar" else "import=build\\OS5\\Mail.jar;build\\OS5\\MailOS46.jar;build\\OS5\\MailOS47.jar;build\\OS5\\MailOS6.jar;build\\OS5\\MailOS6.jar;${api5_path}\\lib\\net_rim_api.jar",
                    "${folder}\\MailStartup\\res\\icons\\blackberryemail.png",
                    "${folder}\\MailStartup\\res\\icons\\blackberryemail_roll.png",
                    "${folder}\\MailStartup\\src\\org\\logicprobe\\LogicMail\\MailStartup.java"
            )
        }
    }
}

tasks.create("signSource6") {
    doLast {
        exec {
            commandLine("${jdk_path}\\javaw.exe",
                    "-jar",
                    "${api6_path}\\bin\\SignatureTool.jar",
                    "-a",
                    "-p",
                    password,
                    "-r",
                    "${folder}/build/OS6"
            )
            workingDir(api6_path)
        }
        delete("${folder}/build/OS6/cache")
    }
}
tasks.create<Copy>("copy6") {
    dependsOn(tasks.getByName("signSource6"))
    from("build/OS6")
    into("build/OS6/cache")
    include("*.cod", "*.jad")
}

task("Merge6") {
    dependsOn(tasks.getByName("copy6"))
    doLast {
        Thread.sleep(2000)
        exec {
            commandLine(
                    "${api6_path}\\bin\\UpdateJad.exe",
                    "-n",
                    "${folder}\\build\\OS6\\cache\\Email.jad",
                    "${folder}\\build\\OS6\\cache\\Mail.jad",
                    "${folder}\\build\\OS6\\cache\\MailOS5.jad",
                    "${folder}\\build\\OS6\\cache\\MailOS46.jad",
                    "${folder}\\build\\OS6\\cache\\MailOS47.jad",
                    "${folder}\\build\\OS6\\cache\\MailOS6.jad"
            )
            workingDir("${folder}/build/OS6/cache")
        }
        delete("${folder}\\build\\OS6\\cache\\Mail.jad")
        delete("${folder}\\build\\OS6\\cache\\MailOS5.jad")
        delete("${folder}\\build\\OS6\\cache\\MailOS46.jad")
        delete("${folder}\\build\\OS6\\cache\\MailOS47.jad")
        delete("${folder}\\build\\OS6\\cache\\MailOS6.jad")
    }
}

tasks.register<Zip>("zip6") {
    dependsOn(tasks.getByName("Merge6"))
    var DependsOn = "MailOS6" //jdp_text.split("[DependsOn\r\n")[1].trim().split("\r\n")[0]
    archiveFileName.set("${DependsOn}-${localVersion}.zip")
    destinationDirectory.set(layout.projectDirectory.dir("build"))
    from("${folder}/build/OS6/cache")
}


tasks.create("signSource") {
    doLast {
        exec {
            commandLine("${jdk_path}\\javaw.exe",
                    "-jar",
                    "${api5_path}\\bin\\SignatureTool.jar",
                    "-a",
                    "-p",
                    password,
                    "-r",
                    "${folder}/build/OS5"
            )
            workingDir(api5_path)
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

task("Merge") {
    dependsOn(tasks.getByName("copy"))
    doLast {
        Thread.sleep(2000)
        exec {
            commandLine(
                    "${api7_path}\\bin\\UpdateJad.exe",
                    "-n",
                    "${folder}\\build\\OS5\\cache\\Email.jad",
                    "${folder}\\build\\OS5\\cache\\Mail.jad",
                    "${folder}\\build\\OS5\\cache\\MailOS5.jad",
                    "${folder}\\build\\OS5\\cache\\MailOS46.jad",
                    "${folder}\\build\\OS5\\cache\\MailOS47.jad"
            )
            workingDir("${folder}/build/OS5/cache")
        }
        delete("${folder}\\build\\OS5\\cache\\Mail.jad")
        delete("${folder}\\build\\OS5\\cache\\MailOS5.jad")
        delete("${folder}\\build\\OS5\\cache\\MailOS46.jad")
        delete("${folder}\\build\\OS5\\cache\\MailOS47.jad")
    }
}

tasks.register<Zip>("zip") {
    dependsOn(tasks.getByName("Merge"))
    var DependsOn = "MailOS5" //jdp_text.split("[DependsOn\r\n")[1].trim().split("\r\n")[0]
    archiveFileName.set("${DependsOn}-${localVersion}.zip")
    destinationDirectory.set(layout.projectDirectory.dir("build"))
    from("${folder}/build/OS5/cache")
}

tasks.register("Release") {
    doLast {
        delete("dist")
        exec {
            commandLine = listOf("git", "clone", "git@github.com:nghuyy/BlackberryMail_Release.git", "dist")
        }
        copy {
            from("build/")
            into("dist/")
            include("*.zip")
        }
        if(File("build/OS5/cache").exists()) {
            copy {
                from("build/OS5/cache")
                into("dist/OS5")
                include("*.cod", "*.jad")
                exclude("Mail.cod")
            }
            copy {
                from(zipTree("build/OS5/cache/Mail.cod"))
                into("dist/OS5")
            }
        }
        if(File("build/OS6/cache").exists()) {
            copy {
                from("build/OS6/cache")
                into("dist/OS6")
                include("*.cod", "*.jad")
                exclude("Mail.cod")
            }
            copy {
                from(zipTree("build/OS6/cache/Mail.cod"))
                into("dist/OS6")
            }
        }

        exec {
            workingDir = File("./dist")
            commandLine = listOf("git", "add", ".")
        }
        exec {
            workingDir = File("./dist")
            commandLine = listOf("git", "commit", "-m", "\"Update\"")
        }
        exec {
            workingDir = File("./dist")
            commandLine = listOf("git", "push", "-f", "origin", "main")
        }
    }
}

task("clean") {
    doLast {
        delete("build")
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

