package com.park.welstory.wooriportal.saramin.enumlist.JobCode;

import lombok.Getter;

@Getter
public enum Code7DrivingTransportDelivery {

    DRIVING(2219, "운전"),
    ONE_TON(662, "1톤"),
    DUMP_TRUCK(678, "덤프트럭"),
    DELIVERY_DRIVER(633, "납품운전원"),
    DESIGNATED_DRIVER(634, "대리운전"),
    TWO_POINT_FIVE_TON(663, "2.5톤"),
    LOWDER(679, "로우더"),
    THREE_POINT_FIVE_TON(664, "3.5톤"),
    MIXER_TRUCK(680, "믹서트럭(레미콘)"),
    RIDER(635, "라이더(배달원)"),
    LOGISTICS_DRIVER(636, "물류기사"),
    FOUR_POINT_FIVE_TON(665, "4.5톤"),
    ARM_ROLL(681, "암롤"),
    DELIVERY_DRIVER2(637, "배송기사"),
    FIVE_TON_ABOVE(666, "5톤이상"),
    ELECTRIC_FORKLIFT(682, "전동지게차"),
    BUS_DRIVER(638, "버스기사"),
    SMALL_CARGO(667, "소형화물"),
    FORKLIFT(683, "지게차"),
    BONDED_TRANSPORT(639, "보세운송"),
    TOW_TRUCK(668, "견인차"),
    CLAW_TRUCK(684, "집게차"),
    COMPANY_CAR_DRIVER(640, "사택기사"),
    VESSEL(669, "선박"),
    CONTAINER_CRANE(685, "컨테이너크레인"),
    MOTORCYCLE(670, "오토바이"),
    CRANE(686, "크레인"),
    SHUTTLE_BUS_DRIVER(641, "셔틀버스기사"),
    EXECUTIVE_DRIVER(642, "수행기사"),
    WING_BODY(671, "윙바디"),
    EXCAVATOR(687, "포크레인(굴삭기)"),
    HOIST(688, "호이스트"),
    CARGO_TRUCK(672, "화물차(카고)"),
    VAN_DRIVER(643, "승합기사"),
    PILOT(644, "조종사"),
    TOP_TRUCK(673, "탑차"),
    TANK_LORRY(674, "탱크로리"),
    GROUND_HANDLING(645, "지상조업"),
    TRUCK(675, "트럭"),
    VEHICLE_ASSISTANT(646, "차량도우미"),
    TRAILER(676, "트레일러"),
    SPECIAL_VEHICLE(677, "특수차량"),
    QUICK_SERVICE(647, "퀵서비스"),
    CAR_TRANSPORT_DRIVER(648, "탁송기사"),
    COURIER_DRIVER(649, "택배기사"),
    TAXI_DRIVER(650, "택시기사"),
    FORWARDER(651, "포워더"),
    MOVING_SERVICE(652, "포장이사"),
    LAND_TRANSPORT(653, "육상운송"),
    LOADING_UNLOADING(654, "적재하역"),
    OWNER_OPERATOR(655, "지입"),
    RAIL_TRANSPORT(656, "철도운송"),
    CUSTOMS_CLEARANCE(657, "통관"),
    AIR_TRANSPORT(658, "항공운송"),
    SEA_TRANSPORT(659, "해상운송"),
    DISPATCH_MANAGEMENT(660, "배차관리"),
    SHIPMENT(661, "선적");

    private final int code;
    private final String name;

    Code7DrivingTransportDelivery(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static Code7DrivingTransportDelivery fromCode(int code) {
        for (Code7DrivingTransportDelivery c : values()) {
            if (c.code == code) return c;
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }
}