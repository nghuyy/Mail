defaultTasks(
        "build",
        "zip"
)
var bb_buildfile = listOf<String>(
        "**/*.cod",
        "**/*.debug",
        "**/*.rapc",
        "**/*.jad",
        "**/*.jar",
        "**/*.export.xml",
        "**/*.csl",
        "**/*.cso"
)
task("build") {
    doLast {
        delete("dist/raw")
        copy {
            from(".")
            into("dist/raw")
            include("*.cod", "*.jad")
        }
        copy {
            from("Mail")
            into("dist/raw")
            include("*.cod", "*.jad")
        }
        copy {
            from("MailStartup")
            into("dist/raw")
            include("*.cod", "*.jad")
        }
    }
}
tasks.register<Zip>("zip") {
    var jdp_text = File("MailStartup/MailStartup.jdp").readText(charset("utf-8"))
    var version = jdp_text.split("Version=")[1].trim()
    var DependsOn = jdp_text.split("[DependsOn\r\n")[1].trim().split("\r\n")[0]
    archiveFileName.set("${DependsOn}-${version}.zip")
    destinationDirectory.set(layout.projectDirectory.dir("dist"))
    from("dist/raw")
}

task("clean") {
    doLast {
        delete(fileTree("Mail").matching {
            include(bb_buildfile)
        })
        delete(fileTree("MailStartup").matching {
            include(bb_buildfile)
        })
        delete("dist")
    }
}