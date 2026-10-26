package org.alexstpm.fms.counterparty.repository;

import org.alexstpm.fms.counterparty.model.Counterparty;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CounterpartyRepository extends JpaRepository<Counterparty, Long> {
}
