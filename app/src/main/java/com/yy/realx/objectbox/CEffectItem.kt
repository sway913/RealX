package com.yy.realx.objectbox

import com.yy.realx.EffectSettings
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
class CEffectItem(
    @Id var id: Long = 0,
    var path: String = "",
    var name: String = "",
    var type: Int = EffectSettings.FEATURE_2D,
    var values: String = ""
)
