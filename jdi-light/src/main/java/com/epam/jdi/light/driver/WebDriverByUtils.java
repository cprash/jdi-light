package com.epam.jdi.light.driver;

import com.epam.jdi.light.elements.interfaces.base.IBaseElement;
import com.jdiai.tools.func.JFunc;
import com.jdiai.tools.func.JFunc1;
import com.jdiai.tools.map.MapArray;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.epam.jdi.light.common.Exceptions.runtimeException;
import static com.epam.jdi.light.driver.WebDriverFactory.getDriver;
import static com.epam.jdi.light.settings.WebSettings.printSmartLocators;
import static com.jdiai.tools.LinqUtils.*;
import static com.jdiai.tools.PrintUtils.print;
import static com.jdiai.tools.ReflectionUtils.isClass;
import static com.jdiai.tools.StringUtils.format;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.*;
import static org.openqa.selenium.support.ui.Quotes.escape;

/**
 * Created by Roman Iovlev on 26.09.2019
 * Email: roman.iovlev.jdi@gmail.com; Skype: roman.iovlev
 */
public final class WebDriverByUtils {

    private WebDriverByUtils() { }

    public static Function<String, By> getByFunc(By by) {
        return first(getMapByTypes(), key -> by.toString().contains(key));
    }

    private static String getBadLocatorMsg(String byLocator, Object... args) {
        return "Bad locator template '" + byLocator + "'. Args: " + print(select(args, Object::toString), ", ", "'%s'") + ".";
    }
    public static By fillByTemplate(By by, Object... args) {
        String byLocator = getByLocator(by);
        if (!byLocator.contains("%"))
            throw new RuntimeException(getBadLocatorMsg(byLocator, args));
        try {
            byLocator = format(byLocator, args);
        } catch (Exception ex) {
            throw new RuntimeException(getBadLocatorMsg(byLocator, args));
        }
        return getByFunc(by).apply(byLocator);
    }

    public static By fillByMsgTemplate(By by, Object... args) {
        String byLocator = getByLocator(by);
        try {
            byLocator = MessageFormat.format(byLocator, args);
        } catch (Exception ex) {
            throw new RuntimeException(getBadLocatorMsg(byLocator, args));
        }
        return getByFunc(by).apply(byLocator);
    }

    public static By copyBy(By by) {
        String byLocator = getByLocator(by);
        return getByFunc(by).apply(byLocator);
    }

    public static String getByLocator(By by) {
        if (by == null) return null;
        String byAsString = by.toString();
        int index = byAsString.indexOf(": ") + 2;
        return byAsString.substring(index);
    }
    private static MapArray<String, String> byReplace = new MapArray<>(new Object[][] {
            {"cssSelector", "css"},
            {"tagName", "tag"},
            {"className", "class"}
    });
    public static String getByType(By by) {
        Matcher m = Pattern.compile("By\\.(?<locator>[a-zA-Z]+):.*").matcher(by.toString());
        if (m.find()) {
            String result = m.group("locator");
            return byReplace.has(result) ? byReplace.get(result) : result;
        }
        throw new RuntimeException("Can't get By name for: " + by);
    }

    public static By correctXPaths(By byValue) {
        return byValue.toString().contains("By.xpath: //") || byValue.toString().contains("By.xpath: (//")
                ? getByFunc(byValue).apply(getByLocator(byValue)
                .replaceFirst("/", "./"))
                : byValue;
    }
    public static String shortBy(By by) {
        return shortBy(by, () -> "No locator");
    }
    public static String shortBy(By by, IBaseElement el) {
        return shortBy(by, () -> printSmartLocators(el));
    }

    private static String shortBy(By by, JFunc<String> noLocator) {
        return (by == null
            ? noLocator.execute()
            : format("%s='%s'", getByType(by), getByLocator(by))).replace("%s", "{{VALUE}}");
    }

    public static By getByFromString(String stringLocator) {
        if (stringLocator == null || stringLocator.equals("")) {
            throw new RuntimeException("Can't get By locator from string empty or null string");
        }
        String[] split = stringLocator.split("(^=)*=.*");
        if (split.length == 1) {
            return NAME_TO_LOCATOR.execute(split[0]);
        }
        switch (split[0]) {
            case "css": return By.cssSelector(split[1]);
            case "xpath": return By.xpath(split[1]);
            case "class": return By.className(split[1]);
            case "name": return By.name(split[1]);
            case "id": return By.id(split[1]);
            case "tag": return By.tagName(split[1]);
            case "link": return By.partialLinkText(split[1]);
            default: throw new RuntimeException(
                format("Can't get By locator from string: %s. Bad suffix: %s. (available: css, xpath, class, id, name, link, tag)",
                        stringLocator, split[0]));
        }
    }

    private static Map<String, Function<String, By>> getMapByTypes() {
        Map<String, Function<String, By>> map = new HashMap<>();
        map.put("By.cssSelector", By::cssSelector);
        map.put("By.className", By::className);
        map.put("By.id", By::id);
        map.put("By.linkText", By::linkText);
        map.put("By.name", By::name);
        map.put("By.partialLinkText", By::partialLinkText);
        map.put("By.tagName", By::tagName);
        map.put("By.xpath", By::xpath);
        return map;
    }
    public static List<WebElement> uiSearch(By by) {
        return uiSearch(getDriver(), by);
    }

    public static List<WebElement> uiSearch(SearchContext ctx, By by) {
        List<WebElement> els = null;
        for (Object step : searchBy(by)) {
            els = getEls(step, ctx, els);
        }
        return els;
    }
    private static List<WebElement> getEls(Object step, SearchContext ctx, List<WebElement> els) {
        if (isClass(step.getClass(), By.class)) {
            String byName = getByType((By) step);
            if (byName.equals("id") || (byName.equals("css") && getByLocator((By) step).matches("^#[a-zA-Z-]+$"))) {
                return getDriver().findElements((By) step);
            } else {
                return els == null
                    ? ctx.findElements((By) step)
                    : selectMany(els, e -> e.findElements((By) step));
            }
        }
        else {
            if (isClass(step.getClass(), Integer.class) && els != null) {
                return singletonList(els.get((Integer) step - 1));
            }
        }
        throw runtimeException("Unknown locator part '%s'. Can't get element. Please correct locator");
    }
    public static List<Object> searchBy(By by) {
        try {
            if (!getByType(by).equals("css")) {
                return singletonList(by);
            }
            String locator = getByLocator(by);
            List<By> result = replaceUp(locator);
            result = replaceText(result);
            return valueOrDefault(replaceChildren(result), one(by));
        } catch (Exception ex) { throw new RuntimeException("Search By failed"); }
    }

    public static JFunc1<String, By> NAME_TO_LOCATOR = WebDriverByUtils::defineLocator;

    public static By defineLocator(String locator) {
        String by = locator.contains("*root*")
            ? locator.replace("*root*", "")
            : locator;
        if (isBlank(by)) {
            return By.cssSelector("");
        }
        if (by.length() == 1) {
            return By.cssSelector(locator);
        }
        if (by.matches("\\*=.*")) {
            return withText(by.substring(2));
        }
        if (by.matches("=.*")) {
            return byText(by.substring(1));
        }
        return by.substring(0,2).contains("/") || by.contains("..") || by.charAt(0) == '('
            ? By.xpath(locator)
            : By.cssSelector(locator);
    }

    private static List<Object> one(By by) {
        List<Object> result = new ArrayList<>();
        result.add(by);
        return result;
    }

    private static List<By> replaceUp(String locator) {
        List<By> result = null;
        if (locator.contains("<")) {
            result = new ArrayList<>();
            String loc = locator.replaceAll(" *< *", "<");
            Matcher m = Pattern.compile("(?<up><+)").matcher(loc);
            while (m.find()) {
                String[] locs = loc.split(m.group("up"));
                if (locs.length > 0) {
                    result.add(By.cssSelector(locs[0]));
                }
                result.add(getUpXpath(m.group("up")));
                loc = locs.length == 2 ? locs[1] : "";
            }
            if (isNotBlank(loc))
                result.add(By.cssSelector(loc));
        }
        return valueOrDefault(result, singletonList(By.cssSelector(locator)));
    }

    private static By getUpXpath(String group) {
        String result = ".." + repeat("/..", group.length()-1);
        return By.xpath(result);
    }

    private static List<By> replaceText(List<By> bys) {
        List<By> result = new ArrayList<>();
        for (By by : bys) {
            if (getByType(by).equals("css")) {
                result.addAll(replaceText(getByLocator(by)));
            } else {
                result.add(by);
            }
        }
        return result;
    }

    private static List<By> replaceText(String locator) {
        List<By> result = new ArrayList<>();
        String loc = locator;
        Matcher m = Pattern.compile("\\[(?<modifier>\\*?)'(?<text>[^']+)']").matcher(loc);
        while (m.find() && isNotEmpty(loc)) {
            String[] locs = loc.split("\\[\\*?'"+m.group("text")+"']");
            if (locs.length > 0) {
                result.add(By.cssSelector(locs[0]));
            }
            result.add(m.group("modifier").equals("")
                ? By.xpath(".//*[text()='" + m.group("text") + "']")
                : By.xpath(".//*[contains(text(),'" + m.group("text") + "')]"));
            loc = locs.length == 2 ?  locs[1] : "";
        }
        if (isNotEmpty(loc)) {
            result.add(By.cssSelector(loc));
        }
        return result;
    }

    private static List<Object> replaceChildren(List<By> bys) {
        List<Object> result = new ArrayList<>();
        for (By by : bys) {
            if (getByType(by).equals("css")) {
                result.addAll(replaceChildren(getByLocator(by)));
            } else {
                result.add(by);
            }
        }
        return result;
    }

    private static List<Object> replaceChildren(String locator) {
        List<Object> result = new ArrayList<>();
        String loc = locator;
        Matcher m = Pattern.compile("\\[(?<num>\\d+)]").matcher(loc);
        while (m.find() && isNotEmpty(loc)) {
            String[] locs = loc.split("\\["+m.group("num")+"]");
            if (locs.length > 0) {
                result.add(By.cssSelector(locs[0]));
            }
            result.add(Integer.parseInt(m.group("num")));
            loc = locs.length == 2 ?  locs[1] : "";
        }
        if (isNotEmpty(loc)) {
            result.add(By.cssSelector(loc));
        }
        return result;
    }
    public static String asTextLocator(String text) {
        return format(".//*/text()[normalize-space(.) = %s]/parent::*", escape(text));
    }
    public static String asContainsTextLocator(String text) {
        return format(".//*/text()[contains(normalize-space(.), %s)]/parent::*", escape(text));
    }
    public static By byText(String text) {
        return By.xpath(asTextLocator(text));
    }
    public static By withText(String text) {
        return By.xpath(asContainsTextLocator(text));
    }
}
