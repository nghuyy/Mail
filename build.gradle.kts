defaultTasks(
        "build",
        "zip"
)
task("build") {
    doLast {
        copy {
            from(".")
            into("dist/raw")
            include("*.cod", "*.jad")
        }

    }
}

tasks.register<Zip>("zip") {
    var jdp_text = File("MailStartup.jdp").readText(charset("utf-8"))
    var version = jdp_text.split("Version=")[1].trim()
    archiveFileName.set("${version}.zip")
    destinationDirectory.set(layout.projectDirectory.dir("dist"))
    from("dist/raw")
}