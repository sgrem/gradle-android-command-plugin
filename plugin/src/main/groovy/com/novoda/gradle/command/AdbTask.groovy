package com.novoda.gradle.command

import groovy.transform.Memoized


public class AdbTask extends org.gradle.api.DefaultTask {

    final pluginEx = project.android.extensions.findByType(AndroidCommandPluginExtension)

    AdbCommand adbCommand;

    // set automatically by VariantConfigurator
    def apkPath

    // set automatically by VariantConfigurator
    def variationName

    def deviceId

    @Memoized
    def getDeviceId() {
        if (deviceId instanceof Closure)
            deviceId = deviceId.call()
        deviceId ?: pluginEx.deviceId
    }

    def getPackageName() {
        def output = readApkProperty('package')
        if (output) {
            def matcher = output.readLines()[0] =~ /name='(.*?)'/
            if (matcher) {
                matcher[0][1]
            }
        } else {
            throw new IllegalArgumentException("Could not read 'package' property of $apkPath")
        }
    }

    protected assertDeviceAndRunCommand(def parameters) {
        assertDeviceConnected()
        runCommand(parameters)
    }

    protected void assertDeviceConnected() {
        def id = getDeviceId()
        Device device = pluginEx.devices().find { device -> device.id == id }
        if (!device)
            throw new IllegalStateException("No device with ID $id found.")
        printDeviceInfo(device)
    }

    private printDeviceInfo(device) {
        println '=========================='
        println device.toString()
        println '=========================='
    }

    protected void runCommand(def parameters) {
        adbCommand.deviceId = getDeviceId()
        adbCommand.parameters = parameters
        logger.info "running command: $adbCommand"
        handleCommandOutput(adbCommand.execute().text)
    }

    protected handleCommandOutput(def text)  {
        logger.info text
    }

    protected final readApkProperty(String propertyKey) {
        if (!apkPath) {
            throw new IllegalStateException("No APK found for the '$name' task")
        }
        String output = [pluginEx.aapt, 'dump', 'badging', apkPath].execute().text.readLines().find {
            it.startsWith("$propertyKey:")
        }
        output
    }
}
