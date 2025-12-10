package com.japanesestudy.app.repository;

import com.japanesestudy.app.model.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemTypeRepository extends JpaRepository<ItemType, Long> {
    java.util.Optional<ItemType> findByNameIgnoreCase(String name);
}
