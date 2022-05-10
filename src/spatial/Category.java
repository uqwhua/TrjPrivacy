package spatial;

import java.util.LinkedList;
import java.util.List;

public class Category {

    // the prefix of category codes
    public static final String[] Food = new String[]{"1"};
    public static final String[] Mall = new String[]{"2"};
    public static final String[] Medical = new String[]{"2800", "72", "75"};
    public static final String[] Vehicle = new String[]{"4", "7100", "7103"};
    public static final String[] Entertainment = new String[]{"5", "6"};    // 5 is hotel
    public static final String[] Government = new String[]{"70", "7101", "7102"};
    public static final String[] Culture = new String[]{"7180", "74", "94", "9600", "9700", "A880"};
    public static final String[] Scenery = new String[]{"73", "9080", "9180", "92", "93", "95"};
    public static final String[] Business = new String[]{"76", "AF"};
    public static final String[] Residence = new String[]{"7700", "A900"};
    public static final String[] Life = new String[]{"78", "A080", "A2", "A3", "A4", "A600", "AB"};
    public static final String[] Suburbs = new String[]{"808A", "808B", "9800", "BB", "BF"};    //BF00 is bridge
    public static final String[] Transport = new String[]{"8"}; // !!!except suburbs
    public static final String[] Bank = new String[]{"A1"};
    public static final String[] School = new String[]{"A70"};
    public static final String[] Company = new String[]{"A980", "A983", "AA"};
    public static final String[] Factory = new String[]{"A982"};

    public static final List<String[]> BeijingCategoryList = new LinkedList<>();

    public static void initialize(){
        BeijingCategoryList.add(Food);
        BeijingCategoryList.add(Mall);
        BeijingCategoryList.add(Medical);
        BeijingCategoryList.add(Vehicle);
        BeijingCategoryList.add(Entertainment);
        BeijingCategoryList.add(Government);
        BeijingCategoryList.add(Culture);
        BeijingCategoryList.add(Scenery);
        BeijingCategoryList.add(Business);
        BeijingCategoryList.add(Residence);
        BeijingCategoryList.add(Life);
        BeijingCategoryList.add(Suburbs);
        BeijingCategoryList.add(Transport);
        BeijingCategoryList.add(Bank);
        BeijingCategoryList.add(School);
        BeijingCategoryList.add(Company);
        BeijingCategoryList.add(Factory);
    }

    public static int getCategoryID(String subCategoryCode) {
        int cid = -1;
        for (int i = 0; i < BeijingCategoryList.size(); i++) {
            String[] category = BeijingCategoryList.get(i);
            for(String prefix: category) {
                if (prefix.length() == 4) {   // it's a whole code of the category instead of prefix
                    if (subCategoryCode.equals(prefix)) {
                        return i;
                    }
                } else if (subCategoryCode.startsWith(prefix)) {
                    return i;
                }
            }
        }
        return cid;
    }

    public static int getNum() {
        return BeijingCategoryList.size();
    }
}
