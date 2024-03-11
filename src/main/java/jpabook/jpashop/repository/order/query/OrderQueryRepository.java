package jpabook.jpashop.repository.order.query;

import jakarta.persistence.EntityManager;
import jpabook.jpashop.dto.OrderFlatDto;
import jpabook.jpashop.dto.OrderItemQueryDto;
import jpabook.jpashop.dto.OrderQueryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class OrderQueryRepository {

    private final EntityManager em;

    // ToOne 관계 조회
    private List<OrderQueryDto> findOrders() {
        return em.createQuery(
                "select new jpabook.jpashop.dto.OrderQueryDto(o.id, m.name, o.orderDate, o.status, d.address)" +
                " from Order o" +
                " join o.member m" +
                " join o.delivery d", OrderQueryDto.class)
                .getResultList();
    }

    // ToMany 관계 조회
    private List<OrderItemQueryDto> findOrderItems(Long orderId) {
        return em.createQuery(
                "select new jpabook.jpashop.dto.OrderItemQueryDto(oi.order.id, i.name, oi.orderPrice, oi.count)" +
                " from OrderItem oi" +
                " join oi.item i " +
                " where oi.order.id = :orderId", OrderItemQueryDto.class)
                .setParameter("orderId", orderId)
                .getResultList();
    }

    // orderId 리스트 추출
    private List<Long> toOrderIds(List<OrderQueryDto> result) {
        return result.stream()
                .map(OrderQueryDto::getOrderId)
                .toList();
    }

    // Map 을 사용하여 orderItems 조회
    private Map<Long, List<OrderItemQueryDto>> findOrderItemMap(List<Long> orderIds) {

        List<OrderItemQueryDto> orderItems = em.createQuery("select new jpabook.jpashop.dto.OrderItemQueryDto(oi.order.id, i.name, oi.orderPrice, oi.count)" +
                        " from OrderItem oi " +
                        " join oi.item i" +
                        " where oi.order.id in :orderIds", OrderItemQueryDto.class)
                .setParameter("orderIds", orderIds)
                .getResultList();

        // orderItems 들을 OrderId로 그룹화하여 리턴하면 됨
        return orderItems.stream().collect(Collectors.groupingBy(OrderItemQueryDto::getOrderId));
    }



    // JPA 에서 직접 DTO 조회하기 - 쿼리 : 루트 1번, 컬렉션 N 번
    // ToMany 관계는 별도로 조회
    // 단건 조회에서 많이 사용함
    public List<OrderQueryDto> findOrderQueryDtos() {
        List<OrderQueryDto> result = findOrders(); // 먼저 ToOne 조회

        result.forEach(o -> {
            List<OrderItemQueryDto> orderItems = findOrderItems(o.getOrderId());
            o.setOrderItems(orderItems);
        });

        return result;
    }

    // 최적화 ( 쿼리는 루트 1번, 컬렉션 1번 )
    // 데이터를 한 꺼번에 처리할 때 많이 사용함
    public List<OrderQueryDto> findAllByDto_optimization() {
        List<OrderQueryDto> result = findOrders();

        // orderItems 를 Map 을 사용하여 한 번에 조회
        Map<Long, List<OrderItemQueryDto>> orderItems = findOrderItemMap(toOrderIds(result));

        // 루프 돌면서 컬렉 추가 ( 추가적인 쿼리 실행 x )
        result.forEach(o -> o.setOrderItems(orderItems.get(o.getOrderId())));

        return result;
    }

    public List<OrderFlatDto> findAllByDto_flat() {
        return em.createQuery(
                        "select new jpabook.jpashop.dto.OrderFlatDto(o.id, m.name, o.orderDate, " +
                                "o.status, d.address, i.name, oi.orderPrice, oi.count)" +
                                " from Order o" +
                                " join o.member m" +
                                " join o.delivery d" +
                                " join o.orderItems oi" +
                                " join oi.item i", OrderFlatDto.class)
                .getResultList();
    }
}
