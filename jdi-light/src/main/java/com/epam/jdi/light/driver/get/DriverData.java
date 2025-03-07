package com.epam.jdi.light.driver.get;

import com.jdiai.tools.func.JAction;
import com.jdiai.tools.func.JAction1;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.ie.InternetExplorerOptions;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.opera.OperaOptions;
import org.openqa.selenium.safari.SafariOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import static com.epam.jdi.light.driver.WebDriverUtils.maximizeWindow;
import static com.epam.jdi.light.driver.get.OsTypes.*;
import static com.epam.jdi.light.settings.JDISettings.COMMON;
import static com.epam.jdi.light.settings.JDISettings.DRIVER;
import static com.epam.jdi.light.settings.WebSettings.logger;
import static com.jdiai.tools.LinqUtils.safeException;
import static com.jdiai.tools.PathUtils.mergePath;
import static com.jdiai.tools.PrintUtils.print;
import static com.jdiai.tools.ReflectionUtils.isClass;
import static com.jdiai.tools.ReflectionUtils.stringToPrimitive;
import static com.jdiai.tools.StringUtils.LINE_BREAK;
import static com.jdiai.tools.StringUtils.format;
import static com.jdiai.tools.switcher.SwitchActions.*;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.openqa.selenium.UnexpectedAlertBehaviour.ACCEPT;
import static org.openqa.selenium.ie.InternetExplorerDriver.UNEXPECTED_ALERT_BEHAVIOR;
import static org.openqa.selenium.logging.LogType.PERFORMANCE;
import static org.openqa.selenium.remote.CapabilityType.*;

/**
 * Created by Roman Iovlev on 26.09.2019
 * Email: roman.iovlev.jdi@gmail.com; Skype: roman.iovlev
 */
public class DriverData {
    public static String getDriverFolder() {
        return isNotBlank(DRIVER.path) && !DRIVER.path.equalsIgnoreCase("default")
            ? DRIVER.path : mergePath(COMMON.testPath, "resources", "drivers");
    }
    public static final String DEFAULT_DRIVER = "chrome";
    public static String ARGUMENTS_PROPERTY = "arguments";

    public static String chromeDriverPath() {
        return mergePath(getDriverFolder(), getOs() == WIN ? "chromedriver.exe" : "chromedriver");
    }
    public static String ieDriverPath() {
        return mergePath(getDriverFolder(),"IEDriverServer.exe");
    }
    public static String edgeDriverPath() {
        return mergePath(getDriverFolder(),"MicrosoftWebDriver.exe");
    }
    public static String operaDriverPath() {
        return driverPath("operadriver");
    }
    public static String safariDriverPath() {
        return "/usr/bin/safaridriver";
    }
    public static String firefoxDriverPath() {
        return driverPath("geckodriver");
    }
    private static String driverPath(String driverName) {
        return mergePath(getDriverFolder(), getOs() == WIN ? driverName + ".exe" : driverName);
    }
    public static OsTypes getOs() {
        if (DRIVER.os != null)
            return DRIVER.os;
        String osName = System.getProperty("os.name").toLowerCase();
        return Switch(osName).get(
            Case(os -> os.contains("mac"), MAC),
            Case(os -> os.contains("win") || os.contains("ms"), WIN),
            Default(LINUX)
        );
    }

    // GET DRIVER
    public static WebDriver driverSettings(WebDriver driver) {
        if (DRIVER.screenSize.maximize) {
            maximizeWindow(driver);
        } else {
            driver.manage().window().setSize(DRIVER.screenSize.asDimension());
        }
        return driver;
    }

    public static MutableCapabilities getCapabilities(
            MutableCapabilities caps, JAction1<MutableCapabilities> defaultCapabilities) {
        try {
            setupErrors.clear();
            defaultCapabilities.execute(caps);
        } catch (Throwable ex) {
            setupErrors.add("Setup caps exception: " + safeException(ex));
        }
        if (isNotEmpty(setupErrors)) {
            logger.info("Failed to set Default Capabilities for Driver:");
            logger.info("Errors: " + print(setupErrors, LINE_BREAK));
            setupErrors.clear();
        }
        try {
            DRIVER.capabilities.common.forEach((property, value) -> setupCapability(caps, property, value));
        } catch (Throwable ex) {
            logger.info("Failed to set COMMON_CAPABILITIES Capabilities for Driver: " + safeException(ex));
        }
        return caps;
    }

    public static void setupCapability(MutableCapabilities cap, String property, String value) {
        if(!property.equals(ARGUMENTS_PROPERTY)) {
            if (property.toLowerCase().contains("version")) {
                cap.setCapability(property, value);
            } else {
                cap.setCapability(property, stringToPrimitive(value));
            }
        } else {
            if (isClass(cap.getClass(), ChromeOptions.class)) {
                ((ChromeOptions) cap).addArguments(value.split(" "));
            } else if (isClass(cap.getClass(), FirefoxOptions.class)) {
                ((FirefoxOptions) cap).addArguments(value.split(" "));
            }
        }
    }

    public static List<String> setupErrors = new ArrayList<>();
    public static void setUp(String name, JAction action) {
        try {
            action.invoke();
        } catch (Throwable ex) {
            setupErrors.add(format("%s: %s", name, safeException(ex)));
        }
    }
    public static void defaultChromeOptions(ChromeOptions cap) {
        HashMap<String, Object> chromePrefs = new HashMap<>();
        setUp("Set Chrome Prefs", () -> {
            chromePrefs.put("credentials_enable_service", false);
            new File(DRIVER.downloadsFolder).mkdirs();
            chromePrefs.put("download.default_directory", DRIVER.downloadsFolder);
            chromePrefs.put("profile.default_content_setting_values.notifications", 0);
            chromePrefs.put("profile.default_content_settings.popups", 0);
            chromePrefs.put("profile.password_manager_enabled", false);
        });
        setUp("Chrome: '--disable-web-security', '--disable-extensions', 'test-type'",
            () -> cap.addArguments("--disable-web-security", "--disable-extensions", "test-type"));
        setUp("Chrome: PageLoadStrategy:" + DRIVER.pageLoadStrategy,
            () -> cap.setPageLoadStrategy(DRIVER.pageLoadStrategy));
        setUp("Chrome: ACCEPT_SSL_CERTS:true",
            () -> cap.setCapability(ACCEPT_SSL_CERTS, true));
        setUp("Chrome: " + UNEXPECTED_ALERT_BEHAVIOR + "=" + ACCEPT,
            () -> cap.setCapability(UNEXPECTED_ALERT_BEHAVIOR, ACCEPT));
        setUp("Chrome: setExperimentalOption: prefs",
            () -> cap.setExperimentalOption("prefs", chromePrefs));
        setUp("Chrome: setUpExperimentalOption: prefs",
            () -> {
                LoggingPreferences logPrefs = new LoggingPreferences();
                logPrefs.enable(PERFORMANCE, Level.ALL);
                cap.setCapability(LOGGING_PREFS, logPrefs);
                cap.setCapability("goog:loggingPrefs", logPrefs);
            });
        // Capabilities from settings
        DRIVER.capabilities.chrome.forEach((property, value) -> setupCapability(cap, property, value));
    }
    public static JAction1<ChromeOptions> CHROME_OPTIONS = DriverData::defaultChromeOptions;

    public static void defaultFirefoxOptions(FirefoxOptions cap) {
        FirefoxProfile firefoxProfile = new FirefoxProfile();
        setUp("Set FirefoxProfile", () -> {
            firefoxProfile.setAssumeUntrustedCertificateIssuer(false);
            firefoxProfile.setPreference("browser.download.folderList", 2);
            firefoxProfile.setPreference("browser.download.manager.showWhenStarting", false);
            firefoxProfile.setPreference("browser.helperApps.alwaysAsk.force", false);
            firefoxProfile.setPreference("browser.helperApps.neverAsk.saveToDisk", "application/xls;text/csv;text/plain");
            firefoxProfile.setPreference("browser.download.dir", DRIVER.downloadsFolder);
            firefoxProfile.setPreference("print.always_print_silent", "true");
            firefoxProfile.setPreference("print.show_print_progress", "false");
            firefoxProfile.setPreference("browser.startup.homepage", "about:blank");
            firefoxProfile.setPreference("startup.homepage_welcome_url", "about:blank");
            firefoxProfile.setPreference("startup.homepage_welcome_url.additional", "about:blank");
            firefoxProfile.setPreference("network.http.phishy-userpass-length", 255);
        });
        setUp("Firefox: PageLoadStrategy:" + DRIVER.pageLoadStrategy,
            () -> cap.setPageLoadStrategy(DRIVER.pageLoadStrategy));
        setUp("Firefox: ACCEPT_SSL_CERTS: true",
            () -> cap.setCapability(ACCEPT_SSL_CERTS, true));
        setUp("Firefox: UNEXPECTED_ALERT_BEHAVIOR, ACCEPT",
            () -> cap.setCapability(UNEXPECTED_ALERT_BEHAVIOR, ACCEPT));
        setUp("Firefox: Firefox Profile",
            () -> cap.setProfile(firefoxProfile));
        // Capabilities from settings
        DRIVER.capabilities.firefox.forEach((property, value) -> setupCapability(cap, property, value));
    }
    public static JAction1<FirefoxOptions> FIREFOX_OPTIONS = DriverData::defaultFirefoxOptions;

    public static void defaultIEOptions(InternetExplorerOptions cap) {
        setUp("IE: introduceFlakinessByIgnoringSecurityDomains",
            cap::introduceFlakinessByIgnoringSecurityDomains);
        setUp("ignoreZoomSettings",
            cap::ignoreZoomSettings);
        setUp("IE: requireWindowFocus:true",
            () -> cap.setCapability("requireWindowFocus", true));
        setUp("IE: PageLoadStrategy:" + DRIVER.pageLoadStrategy,
            () -> cap.setPageLoadStrategy(DRIVER.pageLoadStrategy));
        setUp("IE: takeFullPageScreenshot",
            cap::takeFullPageScreenshot);
        setUp("IE: ACCEPT_SSL_CERTS: true",
            () -> cap.setCapability(ACCEPT_SSL_CERTS, true));
        setUp("IE: destructivelyEnsureCleanSession",
            cap::destructivelyEnsureCleanSession);
        setUp("IE: UNEXPECTED_ALERT_BEHAVIOR: ACCEPT)",
            () -> cap.setCapability(UNEXPECTED_ALERT_BEHAVIOR, ACCEPT));
        setUp("IE: SUPPORTS_JAVASCRIPT",
            () -> cap.is(SUPPORTS_JAVASCRIPT));
        setUp("IE: ACCEPT_SSL_CERTS: true",
            () -> cap.setCapability(ACCEPT_SSL_CERTS, true));
        // Capabilities from settings
        DRIVER.capabilities.ie.forEach(cap::setCapability);
    }
    public static JAction1<InternetExplorerOptions> IE_OPTIONS = DriverData::defaultIEOptions;

    public static void defaultEdgeOptions(EdgeOptions cap) {
        DRIVER.capabilities.ieEdge.forEach(cap::setCapability);
    }
    public static JAction1<EdgeOptions> EDGE_OPTIONS = DriverData::defaultEdgeOptions;

    public static void defaultOperaOptions(OperaOptions cap) {
        DRIVER.capabilities.opera.forEach(cap::setCapability);
    }
    public static JAction1<OperaOptions> OPERA_OPTIONS = DriverData::defaultOperaOptions;

    public static void defaultSafariOptions(SafariOptions cap) {
        DRIVER.capabilities.safari.forEach(cap::setCapability);
    }
    public static JAction1<SafariOptions> SAFARI_OPTIONS = DriverData::defaultSafariOptions;
}
