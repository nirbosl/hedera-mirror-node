// SPDX-License-Identifier: Apache-2.0

package org.hiero.mirror.web3.evm.store.accessor;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hiero.mirror.web3.evm.utils.EvmTokenUtils.entityIdNumFromEvmAddress;
import static org.hiero.mirror.web3.evm.utils.EvmTokenUtils.toAddress;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.hiero.mirror.common.CommonProperties;
import org.hiero.mirror.common.domain.entity.Entity;
import org.hiero.mirror.common.domain.entity.EntityId;
import org.hiero.mirror.web3.repository.EntityRepository;
import org.hyperledger.besu.datatypes.Address;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EntityDatabaseAccessorTest {
    private static final String HEX = "0x00000000000000000000000000000000000004e4";
    private static final String ALIAS_HEX = "0x67d8d32e9bf1a9968a5ff53b87d777aa8ebbee69";
    private static final Address ADDRESS = Address.fromHexString(HEX);
    private static final Address ALIAS_ADDRESS = Address.fromHexString(ALIAS_HEX);

    private static final Optional<Long> timestamp = Optional.of(1234L);
    private static final Entity mockEntity = mock(Entity.class);
    private static final long NUM = 1252;
    private static final CommonProperties COMMON_PROPERTIES = CommonProperties.getInstance();

    @InjectMocks
    private EntityDatabaseAccessor entityDatabaseAccessor;

    @Mock
    private EntityRepository entityRepository;

    @Test
    void getEntityByAddress() {
        var entityId = EntityId.of(COMMON_PROPERTIES.getShard(), COMMON_PROPERTIES.getRealm(), NUM);
        var address = toAddress(entityId);
        when(entityRepository.findByIdAndDeletedIsFalse(entityIdNumFromEvmAddress(address)))
                .thenReturn(Optional.of(mockEntity));
        assertThat(entityDatabaseAccessor.get(address, Optional.empty()))
                .hasValueSatisfying(entity -> assertThat(entity).isEqualTo(mockEntity));
    }

    @Test
    void getEntityByAddressHistorical() {
        var entityId = EntityId.of(COMMON_PROPERTIES.getShard(), COMMON_PROPERTIES.getRealm(), NUM);
        var address = toAddress(entityId);
        when(entityRepository.findActiveByIdAndTimestamp(entityIdNumFromEvmAddress(address), timestamp.get()))
                .thenReturn(Optional.of(mockEntity));

        assertThat(entityDatabaseAccessor.get(address, timestamp))
                .hasValueSatisfying(entity -> assertThat(entity).isEqualTo(mockEntity));
    }

    @Test
    void getEntityByAlias() {
        when(entityRepository.findByEvmAddressAndDeletedIsFalse(ALIAS_ADDRESS.toArrayUnsafe()))
                .thenReturn(Optional.of(mockEntity));

        assertThat(entityDatabaseAccessor.get(ALIAS_ADDRESS, Optional.empty()))
                .hasValueSatisfying(entity -> assertThat(entity).isEqualTo(mockEntity));
    }

    @Test
    void getEntityByAliasHistorical() {
        when(entityRepository.findActiveByEvmAddressAndTimestamp(ALIAS_ADDRESS.toArrayUnsafe(), timestamp.get()))
                .thenReturn(Optional.of(mockEntity));

        assertThat(entityDatabaseAccessor.get(ALIAS_ADDRESS, timestamp))
                .hasValueSatisfying(entity -> assertThat(entity).isEqualTo(mockEntity));
    }

    @Test
    void evmAddressFromIdReturnZeroWhenNoEntityFound() {
        when(entityRepository.findByIdAndDeletedIsFalse(anyLong())).thenReturn(Optional.empty());

        assertThat(entityDatabaseAccessor.evmAddressFromId(mock(EntityId.class), Optional.empty()))
                .isEqualTo(Address.ZERO);
    }

    @Test
    void evmAddressFromIdReturnZeroWhenNoEntityFoundHistorical() {
        when(entityRepository.findActiveByIdAndTimestamp(0L, timestamp.get())).thenReturn(Optional.empty());

        assertThat(entityDatabaseAccessor.evmAddressFromId(mock(EntityId.class), timestamp))
                .isEqualTo(Address.ZERO);
    }

    @Test
    void evmAddressFromIdReturnAddressFromEntityEvmAddressWhenPresent() {
        when(entityRepository.findByIdAndDeletedIsFalse(anyLong())).thenReturn(Optional.of(mockEntity));
        when(mockEntity.getEvmAddress()).thenReturn(ADDRESS.toArray());

        assertThat(entityDatabaseAccessor.evmAddressFromId(mock(EntityId.class), Optional.empty()))
                .isEqualTo(ADDRESS);
    }

    @Test
    void evmAddressFromIdReturnAddressFromEntityEvmAddressWhenPresentHistorical() {
        when(entityRepository.findByIdAndDeletedIsFalse(anyLong())).thenReturn(Optional.of(mockEntity));
        when(mockEntity.getEvmAddress()).thenReturn(ADDRESS.toArray());

        assertThat(entityDatabaseAccessor.evmAddressFromId(mock(EntityId.class), Optional.empty()))
                .isEqualTo(ADDRESS);
    }

    @Test
    void evmAddressFromIdReturnAliasFromEntityWhenPresentAndNoEvmAddress() {
        when(entityRepository.findByIdAndDeletedIsFalse(anyLong())).thenReturn(Optional.of(mockEntity));
        when(mockEntity.getEvmAddress()).thenReturn(null);
        when(mockEntity.getAlias()).thenReturn(ALIAS_ADDRESS.toArray());

        assertThat(entityDatabaseAccessor.evmAddressFromId(mock(EntityId.class), Optional.empty()))
                .isEqualTo(ALIAS_ADDRESS);
    }

    @Test
    void evmAddressFromIdReturnAliasFromEntityWhenPresentAndNoEvmAddressHistorical() {
        when(entityRepository.findActiveByIdAndTimestamp(0L, timestamp.get())).thenReturn(Optional.of(mockEntity));
        when(mockEntity.getEvmAddress()).thenReturn(null);
        when(mockEntity.getAlias()).thenReturn(ALIAS_ADDRESS.toArray());

        assertThat(entityDatabaseAccessor.evmAddressFromId(mock(EntityId.class), timestamp))
                .isEqualTo(ALIAS_ADDRESS);
    }

    @Test
    void evmAddressFromIdReturnToAddressByDefault() {
        when(entityRepository.findByIdAndDeletedIsFalse(anyLong())).thenReturn(Optional.of(mockEntity));
        when(mockEntity.getEvmAddress()).thenReturn(null);
        when(mockEntity.getAlias()).thenReturn(null);

        final var entityId = EntityId.of(COMMON_PROPERTIES.getShard(), COMMON_PROPERTIES.getRealm(), 3L);
        assertThat(entityDatabaseAccessor.evmAddressFromId(entityId, Optional.empty()))
                .isEqualTo(toAddress(entityId));
    }

    @Test
    void evmAddressFromIdReturnToAddressByDefaultHistorical() {
        final var entityId = EntityId.of(COMMON_PROPERTIES.getShard(), COMMON_PROPERTIES.getRealm(), 3L);
        when(entityRepository.findActiveByIdAndTimestamp(entityId.getId(), timestamp.get()))
                .thenReturn(Optional.of(mockEntity));
        when(mockEntity.getEvmAddress()).thenReturn(null);
        when(mockEntity.getAlias()).thenReturn(null);

        assertThat(entityDatabaseAccessor.evmAddressFromId(entityId, timestamp)).isEqualTo(toAddress(entityId));
    }
}
