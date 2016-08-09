import java.time.Clock

import com.google.inject.AbstractModule
import data.{DASUserOps, SchemeClaimOps, TransientAccessTokenOps}
import db.{DASUserDAO, SchemeClaimDAO, TransientAccessTokenDAO}

class Module extends AbstractModule {

  override def configure() = {
    // Use the system clock as the default implementation of Clock
    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)

    bind(classOf[DASUserOps]).to(classOf[DASUserDAO])
    bind(classOf[SchemeClaimOps]).to(classOf[SchemeClaimDAO])
    bind(classOf[TransientAccessTokenOps]).to(classOf[TransientAccessTokenDAO])
  }
}
