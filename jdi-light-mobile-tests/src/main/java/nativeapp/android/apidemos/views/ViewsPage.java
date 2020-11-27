package nativeapp.android.apidemos.views;

import com.epam.jdi.light.mobile.elements.common.app.Button;
import com.epam.jdi.light.mobile.elements.pageobjects.annotations.MobileFindBy;
import com.epam.jdi.light.mobile.elements.pageobjects.annotations.MobileFindBys;

import java.util.List;

public class ViewsPage {

    @MobileFindBy(accessibilityId = "Buttons")
    public static Button buttonsPage;

    @MobileFindBy(accessibilityId = "Rating Bar")
    public static Button ratingBarPage;

    @MobileFindBys(@MobileFindBy(id = "android:id/list"))
    public static List<Button> buttons;

    @MobileFindBy(accessibilityId = "Seek Bar")
    public static Button seekBarPage;

    @MobileFindBy(accessibilityId = "Search View")
    public static Button searchViewPage;

    @MobileFindBy(accessibilityId = "TextFields")
    public static Button textFieldsPage;

    @MobileFindBy(accessibilityId = "Tabs")
    public static Button tabsPage;

    @MobileFindBy(accessibilityId = "Spinner")
    public static Button spinnerPage;

    @MobileFindBy(accessibilityId = "Expandable Lists")
    public static Button expandableList;

    @MobileFindBy(accessibilityId = "Date Widgets")
    public static Button dateWidgetsPage;
}
