package io.github.epam.html.tests.elements.complex.table;

import com.epam.jdi.light.elements.complex.table.Line;
import io.github.com.entities.MarvelUserSimple;
import io.github.epam.TestsInit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static com.epam.jdi.light.elements.complex.table.Column.inColumn;
import static com.epam.jdi.light.elements.complex.table.matchers.ColumnMatcher.containsValue;
import static com.epam.jdi.light.elements.complex.table.matchers.ColumnMatcher.hasValue;
import static com.epam.jdi.light.settings.JDISettings.ELEMENT;
import static io.github.com.StaticSite.usersPage;
import static io.github.com.pages.UsersPage.usersTable;
import static io.github.epam.html.tests.site.steps.States.shouldBeLoggedIn;
import static io.github.epam.test.data.MarvelHeroes.SPIDER_MAN_SIMPLE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.assertEquals;

public class TableTests implements TestsInit {
    private boolean firstTime = true;
    @BeforeMethod
    public void before() {
        shouldBeLoggedIn();
        usersPage.shouldBeOpened();
        if (firstTime) {
            usersPage.open();
            firstTime = false;
        }
    }

    @Test
    public void tableParamsTest() {
        assertEquals(usersTable.size(), 4);
        assertEquals(usersTable.count(), 6);
        assertThat(usersTable.header(), hasItems("Number", "Type", "User", "Description"));
    }
    @Test
    public void iterationTest() {
        String text = "";
        for (String result : usersTable) {
            text += result.charAt(0);
        }
        assertEquals(text, "123456");
    }
    @Test
    public void previewTest() {
        String value = usersTable.preview();
        assertEquals(value.replace(" ", ""),
            "NumberTypeUserDescription" +
            "1AdminUserManagerRomanWolverineVip" +
            "2AdminUserManagerSergeyIvanSpiderManVip" +
            "3AdminUserManagerVladzimirPunisherVip" +
            "4AdminUserManagerHelenBennettCaptainAmericasomedescriptionVip" +
            "5AdminUserManagerYoshiTannamuriCyclopesomedescriptionVip" +
            "6AdminUserManagerGiovanniRovelliHulksomedescriptionVip");
    }

    @Test
    public void valueTest() {
        String value = usersTable.getValue().replace("\r", "")
            .replace("\n", "*").replace("\\n", "*").replace("* ", "*");
        assertEquals(value,
        "||X||Number|Type|User|Description||*" +
            "||"+i(0)+"||1|Admin*User*Manager|Roman|Wolverine*Vip||*" +
            "||"+i(1)+"||2|Admin*User*Manager|Sergey Ivan|Spider Man*Vip||*" +
            "||"+i(2)+"||3|Admin*User*Manager|Vladzimir|Punisher*Vip||*" +
            "||"+i(3)+"||4|Admin*User*Manager|Helen Bennett|Captain America*some description*Vip||*" +
            "||"+i(4)+"||5|Admin*User*Manager|Yoshi Tannamuri|Cyclope*some description*Vip||*" +
            "||"+i(5)+"||6|Admin*User*Manager|Giovanni Rovelli|Hulk*some description*Vip||*");
    }
    public static String i(int index) {
        return index + ELEMENT.startIndex + "";
    }
    @Test
    public void dataColumnTestIndex() {
        assertEquals(usersTable.row(ELEMENT.startIndex + 1)
            .asData(MarvelUserSimple.class), SPIDER_MAN_SIMPLE);
    }
    @Test
    public void dataFilterTest() {
        assertEquals(usersTable.row(hasValue("Sergey Ivan", inColumn("User")),
                containsValue("User", inColumn("Type")))
            .asData(MarvelUserSimple.class), SPIDER_MAN_SIMPLE);
    }
    @Test
    public void allDataFilterTest() {
        List<Line> filteredData = usersTable.rows(containsValue("Ivan", inColumn("User")));
        assertEquals(filteredData.size(), 1);
        assertEquals(filteredData.get(0).asData(MarvelUserSimple.class), SPIDER_MAN_SIMPLE);
    }
    @Test
    public void commonMatchersTest() {
        usersTable.is().displayed();
        usersTable.has().size(6);
        usersTable.assertThat().size(greaterThan(3));
        usersTable.is().notEmpty().size(lessThanOrEqualTo(6));
    }

    // Compare Matchers
    @Test
    public void rowMatcherTest() {
        usersTable.has().rowThat(containsValue("Ivan", inColumn("User")));
    }
    @Test
    public void rowsAllTest() {
        usersTable.assertThat().all().rows(containsValue("Vip", inColumn("Description")));
    }
    @Test
    public void noRowsTest() {
        usersTable.assertThat().no().rows(containsValue("Alex", inColumn("User")));
    }
    @Test
    public void atLeastTest() {
        usersTable.assertThat().atLeast(3).rows(containsValue(" ", inColumn("User")));
    }
    @Test
    public void exactMatcherTest() {
        usersTable.assertThat().exact(2).rows(containsValue("R", inColumn("User")));
    }
    @Test
    public void rowDataExactMatcherTest() {
        usersTable.assertThat().exact(1).rows(hasValue("Roman", inColumn("User")));
    }

    @Test
    public void tableChainTest() {
        usersTable.assertThat()
            .displayed().size(6).size(greaterThan(3)).notEmpty()
            .and().rowThat(containsValue("Ivan", inColumn("User")))
            .all().rows(containsValue("Vip", inColumn("Description")))
            .no().rows(containsValue("Alex", inColumn("User")))
            .atLeast(3).rows(containsValue(" ", inColumn("User")))
            .exact(2).rows(containsValue("R", inColumn("User")))
            .exact(1).rows(hasValue("Roman", inColumn("User")));
    }
}
