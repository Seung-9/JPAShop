package jpabook.jpashop.dto;

import jpabook.jpashop.domain.OrderItem;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItemDto {

    private String itemName;
    private int orderPrice;
    private int orderCount;

    public OrderItemDto(OrderItem orderItem) {
        this.itemName = orderItem.getItem().getName();
        this.orderPrice = orderItem.getOrderPrice();
        this.orderCount = orderItem.getCount();
    }
}
