package it.ute.QAUTE.repository;

import it.ute.QAUTE.entity.Profiles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfilesRepository extends JpaRepository<Profiles, Integer> {
}