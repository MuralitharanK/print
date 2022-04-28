package io.mosip.print.repository;

import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;
import io.mosip.print.entity.MspCardEntity;
import org.springframework.stereotype.Repository;

@Repository("mspCardRepository")
public interface MspCardRepository extends BaseRepository<MspCardEntity, String> {
    
}
