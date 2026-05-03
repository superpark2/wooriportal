package com.park.welstory.wooriportal.saramin.enumlist.JobCode;

import lombok.Getter;

@Getter
public enum Code12ProductPlanningMD {

    FOOD_MD(2245, "식품MD"),
    FASHION_MD(2237, "패션MD"),
    PLANNING_MD(1212, "기획MD"),
    PROCESSED_FOOD(1223, "가공식품"),
    RETAIL_MD(1213, "리테일MD"),
    FURNITURE(1224, "가구"),
    BUYING_MD(1214, "바잉MD"),
    HEALTH_FUNCTIONAL_FOOD(1225, "건강기능식품"),
    BRAND_MD(1215, "브랜드MD"),
    STOCKOUT_MANAGEMENT(1226, "결품관리"),
    SUPERVISOR(1216, "슈퍼바이저"),
    PURCHASE_GENERAL(1227, "구매총괄"),
    SALES_MD(1217, "영업MD"),
    MENS_CLOTHING(1228, "남성의류"),
    OFFLINE_MD(1218, "오프라인MD"),
    DELIVERY_MANAGEMENT(1229, "납기관리"),
    ONLINE_MD(1219, "온라인MD"),
    ROAD_SHOP(1230, "로드샵"),
    LIVING(1231, "리빙"),
    DISTRIBUTION_MD(1220, "유통MD"),
    AMD(1221, "AMD"),
    REVENUE_MANAGEMENT(1232, "매출관리"),
    DUTY_FREE_SHOP(1233, "면세점"),
    VMD(1222, "VMD"),
    STATIONERY(1234, "문구"),
    DEPARTMENT_STORE(1235, "백화점"),
    BRAND_MANAGEMENT(1236, "브랜드관리"),
    BRAND_PLANNING(1237, "브랜드기획"),
    BRAND_LAUNCHING(1238, "브랜드런칭"),
    BRAND_EXPANSION(1239, "브랜드확장"),
    BRANDING(1240, "브랜딩"),
    PRODUCT_MANAGEMENT(1241, "상품관리"),
    PRODUCT_ANALYSIS(1242, "상품분석"),
    DAILY_SUPPLIES(1243, "생활용품"),
    SOCIAL_COMMERCE(1244, "소셜커머스"),
    SHOPPING_MALL(1245, "쇼핑몰"),
    SPORTS_GOODS(1246, "스포츠용품"),
    SPORTS_CLOTHING(1247, "스포츠의류"),
    MARKET_RESEARCH(1248, "시장조사"),
    RETAIL_DISTRIBUTION(1249, "시판"),
    FOOD(1250, "식품"),
    CHILDRENS_WEAR(1251, "아동복"),
    ITEM_SELECTION(1252, "아이템선정"),
    WOMENS_CLOTHING(1253, "여성의류"),
    YOUNG_CASUAL(1254, "영캐주얼"),
    OPEN_MARKET(1255, "오픈마켓"),
    TOYS(1256, "완구"),
    BABY_PRODUCTS(1257, "유아용품"),
    E_COMMERCE(1258, "이커머스"),
    OWN_MALL_MANAGEMENT(1259, "자사몰관리"),
    ELECTRONICS(1260, "전자제품"),
    PRODUCTION_MANAGEMENT(1261, "제작관리"),
    KITCHEN(1262, "주방"),
    JEWELRY_ACCESSORY(1263, "주얼리액세서리"),
    CHANNEL_MANAGEMENT(1264, "채널관리"),
    TREND_ANALYSIS(1265, "트렌드분석"),
    SALES_STRATEGY(1266, "판매전략"),
    POPUP_STORE_MANAGEMENT(1267, "팝업스토어관리"),
    FASHION_BRAND(1268, "패션브랜드"),
    FASHION_ACCESSORY(1269, "패션잡화"),
    FURNISHING(1270, "퍼니싱"),
    EDIT_SHOP(1271, "편집샵"),
    PROMOTION_PLANNING(1272, "프로모션기획"),
    HOME_SHOPPING(1273, "홈쇼핑"),
    HOME_FASHION_DECO(1274, "홈패션홈데코"),
    COSMETICS(1275, "화장품"),
    MEMBER_ANALYSIS(1276, "회원분석"),
    CS_MANAGEMENT(1277, "CS관리"),
    POP(1278, "POP"),
    SNS(1279, "SNS"),
    SRM(1280, "SRM");

    private final int code;
    private final String name;

    Code12ProductPlanningMD(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static Code12ProductPlanningMD fromCode(int code) {
        for (Code12ProductPlanningMD c : values()) {
            if (c.code == code) return c;
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }
}