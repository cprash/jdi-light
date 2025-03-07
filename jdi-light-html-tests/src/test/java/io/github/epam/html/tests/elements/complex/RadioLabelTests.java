package io.github.epam.html.tests.elements.complex;

import io.github.epam.TestsInit;
import org.openqa.selenium.By;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.epam.jdi.light.settings.JDISettings.ELEMENT;
import static io.github.com.StaticSite.metalAndColorsPage;
import static io.github.com.pages.MetalAndColorsPage.odds;
import static io.github.epam.html.tests.site.steps.States.shouldBeLoggedIn;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.assertEquals;

public class RadioLabelTests implements TestsInit {

    @BeforeMethod
    public void before() {
        shouldBeLoggedIn();
        metalAndColorsPage.shouldBeOpened();
        odds.select(defaultText);
    }
    String defaultText = "5";

    @Test
    public void getValueTest() {
        assertEquals(odds.getValue(), defaultText);
    }

    @Test
    public void selectTest() {
        odds.select("3");
        assertEquals(odds.getValue(), "3");
    }

    @Test
    public void setNullValueTest() {
        String optionName = null;
        odds.select(optionName);
        odds.has().text(defaultText);
    }

    @Test
    public void selectNumTest() {
        odds.select(ELEMENT.startIndex);
        assertEquals(odds.getValue(), "1");
    }
    @Test
    public void selectedTest() {
        assertEquals(odds.selected(), defaultText);
    }
    @Test
    public void valuesTest() {
        assertEquals(odds.values(), asList("1", "3", "5", "7"));
    }

    @Test
    public void isValidationTest() {
        odds.is().selected("5");
        odds.is().values(hasItem("7"));
        odds.is().enabled(hasItems("3", "5"));
    }

    @Test
    public void assertValidationTest() {
        odds.assertThat().values(contains("1", "3", "5", "7"));
    }

    // TODO this is not a test but points to improve
    @Test(enabled = false)
    public void problems() {
        odds.list();
        odds.core().finds(By.xpath("../")).get(1).getTagName();
        odds.core().findElements(By.xpath("../"));
    }
}
