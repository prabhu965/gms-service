package org.codealpha.gmsservice.repositories;

import org.codealpha.gmsservice.entities.Notifications;
import org.springframework.data.repository.CrudRepository;
import java.util.List;


public interface NotificationsRepository extends CrudRepository<Notifications,Long> {
	public List<Notifications> findByUserIdAndRead(Long userId, boolean read);
}