package com.metrics.exportservice.repository;

import com.metrics.exportservice.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, String> {
    Optional<Item> findByItemName(String itemName);
}
