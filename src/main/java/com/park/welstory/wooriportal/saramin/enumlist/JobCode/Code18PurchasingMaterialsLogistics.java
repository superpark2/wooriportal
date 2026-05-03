package com.park.welstory.wooriportal.saramin.enumlist.JobCode;

import lombok.Getter;

@Getter
public enum Code18PurchasingMaterialsLogistics {

    PURCHASING(2227, "구매"),
    QUALITY_MANAGEMENT(2212, "품질관리"),
    THREE_PL_OPERATION(1764, "3PL운영"),
    PURCHASE_MANAGEMENT(1750, "구매관리"),
    LOGISTICS_MANAGEMENT(1751, "물류관리"),
    DEVELOPMENT_PURCHASING(1765, "개발구매"),
    LOGISTICS_CLERK(1752, "물류사무원"),
    CLIENT_MANAGEMENT(1766, "거래처관리"),
    BONDED_AGENT(1753, "보세사"),
    INSPECTION(1767, "검품검수"),
    ESTIMATE_MANAGEMENT(1768, "견적관리"),
    MATERIAL_MANAGEMENT(1754, "자재관리"),
    PURCHASING_AGENCY(1769, "구매대행소싱"),
    INVENTORY_MANAGEMENT(1755, "재고관리"),
    DELIVERY_MANAGEMENT(1770, "납기관리"),
    WAREHOUSE_MANAGEMENT(1756, "창고관리"),
    LOGISTICS_AUTOMATION(1771, "물류자동화"),
    FORWARDER(1757, "포워더"),
    DISPATCH_MANAGEMENT(1772, "배차관리"),
    PURCHASE_PLANNING(1758, "구매기획"),
    BONDED_ZONE_MANAGEMENT(1773, "보세구역관리"),
    INTERNATIONAL_LOGISTICS(1759, "국제물류"),
    LOGISTICS_PLANNING(1760, "물류기획"),
    BONDED_CARGO_MANAGEMENT(1774, "보세화물관리"),
    DISTRIBUTION_MANAGEMENT(1761, "유통관리"),
    LOADING_UNLOADING(1775, "상하차"),
    SRM(1762, "SRM"),
    SHIPMENT(1776, "선적"),
    ORDER_MANAGEMENT(1777, "수발주"),
    SCM(1763, "SCM"),
    SUPPLY_MANAGEMENT(1778, "수급관리"),
    STOCK_MANAGEMENT(1779, "수불관리"),
    MASS_PRODUCTION_PURCHASING(1780, "양산구매"),
    FOREIGN_PURCHASING(1781, "외자구매"),
    OUTSOURCING_MANAGEMENT(1782, "외주관리"),
    COST_MANAGEMENT(1783, "원가관리"),
    RECEIVING(1784, "입고입하"),
    MATERIAL_PURCHASING(1785, "자재구매"),
    CARGO_HANDLING(1786, "적재하역"),
    STRATEGIC_PURCHASING(1787, "전략구매"),
    SETTLEMENT_MANAGEMENT(1788, "정산관리"),
    PROCUREMENT(1789, "조달구매"),
    COLLECTION_SORTING(1790, "집하분류"),
    SHIPPING(1791, "출고출하"),
    PACKING(1792, "패킹(포장)"),
    PICKING(1793, "피킹(집품)"),
    CARGO_MANAGEMENT(1794, "화물관리"),
    ERP(1795, "ERP"),
    MRO(1796, "MRO"),
    WMS(1797, "WMS");

    private final int code;
    private final String name;

    Code18PurchasingMaterialsLogistics(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static Code18PurchasingMaterialsLogistics fromCode(int code) {
        for (Code18PurchasingMaterialsLogistics c : values()) {
            if (c.code == code) return c;
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }
}