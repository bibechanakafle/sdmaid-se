package eu.darken.sdmse.common.areas.modules.priv

import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import eu.darken.sdmse.common.areas.DataArea
import eu.darken.sdmse.common.areas.hasFlags
import eu.darken.sdmse.common.areas.modules.DataAreaModule
import eu.darken.sdmse.common.debug.logging.Logging.Priority.INFO
import eu.darken.sdmse.common.debug.logging.log
import eu.darken.sdmse.common.debug.logging.logTag
import eu.darken.sdmse.common.files.APath
import eu.darken.sdmse.common.files.GatewaySwitch
import eu.darken.sdmse.common.files.canRead
import eu.darken.sdmse.common.files.local.LocalGateway
import eu.darken.sdmse.common.user.UserManager2
import javax.inject.Inject


@Reusable
class DataSystemDEModule @Inject constructor(
    private val userManager2: UserManager2,
    private val gatewaySwitch: GatewaySwitch,
) : DataAreaModule {

    override suspend fun secondPass(firstPass: Collection<DataArea>): Collection<DataArea> {
        val gateway = gatewaySwitch.getGateway(APath.PathType.LOCAL) as LocalGateway

        if (!gateway.hasRoot()) {
            log(TAG, INFO) { "LocalGateway has no root, skipping." }
            return emptySet()
        }

        return firstPass
            .filter { it.type == DataArea.Type.DATA && it.hasFlags(DataArea.Flag.PRIMARY) }
            .map { parentArea ->
                userManager2.allUsers().map { profile ->
                    DataArea(
                        type = DataArea.Type.DATA_SYSTEM_DE,
                        path = parentArea.path.child("system_de", profile.handle.handleId.toString()),
                        userHandle = profile.handle,
                        flags = parentArea.flags,
                    )
                }
            }
            .flatten()
            .filter {
                val canRead = it.path.canRead(gatewaySwitch)
                if (!canRead) log(TAG) { "Can't read $it" }
                canRead
            }
    }

    @Module @InstallIn(SingletonComponent::class)
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: DataSystemDEModule): DataAreaModule
    }

    companion object {
        val TAG: String = logTag("DataArea", "Module", "DataSystemDE")
    }
}