package com.park.welstory.wooriportal.board.sales;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesRepository extends JpaRepository<SalesEntity, Long> {

    Page<SalesEntity> findByCategory(String category, Pageable pageable);
    List<SalesEntity> findByCategory(String category);

}
