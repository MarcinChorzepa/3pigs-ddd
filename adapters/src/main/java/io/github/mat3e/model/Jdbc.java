package io.github.mat3e.model;

import io.github.mat3e.model.event.HouseEvent;
import io.github.mat3e.model.vo.HouseId;
import io.github.mat3e.model.vo.HouseSnapshot;
import io.github.mat3e.model.vo.Material;
import io.github.mat3e.model.vo.Pig;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toUnmodifiableList;

@org.springframework.stereotype.Repository
class HouseRepositoryImpl implements HouseRepository {
    private final JdbcHouseRepository springRepository;

    HouseRepositoryImpl(final JdbcHouseRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public Optional<House> findById(final HouseId houseId) {
        return springRepository.findById(houseId.value())
                .map(HouseEntity::toSnapshot)
                .map(House::from);
    }

    @Override
    public House save(final House aggregate) {
        return House.from(
                springRepository.save(HouseEntity.fromSnapshot(aggregate.getSnapshot()))
                        .toSnapshot()
        );
    }
}

interface JdbcHouseRepository extends Repository<HouseEntity, Integer> {
    Optional<HouseEntity> findById(int id);

    HouseEntity save(HouseEntity toSave);
}

@Table("houses")
class HouseEntity {
    static HouseEntity fromSnapshot(final HouseSnapshot snapshot) {
        var result = new HouseEntity(snapshot.id().value(), snapshot.material(), Pigs.fromList(snapshot.pigs()));
        result.events.addAll(snapshot.events());
        return result;
    }

    @Id
    private final int id;
    private final Material material;
    @Embedded.Empty
    private final Pigs pigs;
    @Transient
    private final Collection<HouseEvent> events = new HashSet<>();

    HouseEntity(final int id, final Material material, final Pigs pigs) {
        this.id = id;
        this.material = material;
        this.pigs = pigs;
    }

    HouseSnapshot toSnapshot() {
        return new HouseSnapshot(HouseId.of(id), material, pigs.toList());
    }

    @DomainEvents
    Collection<HouseEvent> publish() {
        return events;
    }

    @AfterDomainEventPublication
    void clearEvents() {
        events.clear();
    }
}

class Pigs {
    static Pigs fromList(final List<Pig> pigs) {
        var pigsArr = pigs.toArray(new Pig[0]);
        return switch (pigsArr.length) {
            case 0 -> new Pigs(null, null, null);
            case 1 -> new Pigs(pigsArr[0], null, null);
            case 2 -> new Pigs(pigsArr[0], pigsArr[1], null);
            default -> new Pigs(pigsArr[0], pigsArr[1], pigsArr[2]);
        };
    }

    private final Pig first;
    private final Pig second;
    private final Pig third;

    Pigs(final Pig first, final Pig second, final Pig third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    List<Pig> toList() {
        return Stream.of(first, second, third)
                .filter(Objects::nonNull)
                .collect(toUnmodifiableList());
    }
}
