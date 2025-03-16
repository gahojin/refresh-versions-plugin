/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.model

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeSortedWith
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * このテストは、以下のページのテストを参考にしました
 *
 * [Reference Code](https://octopus.com/blog/maven-versioning-explained)
 */
private val versions = listOf(
    Version("NotAVersionSting"),
    Version("1.0-alpha"),
    Version("1.0a1-SNAPSHOT"),
    Version("1.0-alpha1"),
    Version("1.0beta1-SNAPSHOT"),
    Version("1.0-b2"),
    Version("1.0-beta3.SNAPSHOT"),
    Version("1.0-beta3"),
    Version("1.0-milestone1-SNAPSHOT"),
    Version("1.0-m2"),
    Version("1.0-rc1-SNAPSHOT"),
    Version("1.0-cr1"),
    Version("1.0-SNAPSHOT"),
    Version("1.0"),
    Version("1.0-RELEASE"),
    Version("1.0-sp"),
    Version("1.0-a"),
    Version("1.0-whatever"),
    Version("1.0.z"),
    Version("1.0.1"),
    Version("1.0.1.0.0.0.0.0.0.0.0.0.0.0.1")
)

class VersionTest : StringSpec({
    "ソート順が想定通りであること" {
        println(versions.sorted())
        versions shouldBeSortedWith(VersionComparator)
    }

    "エイリアスが処理されていること" {
        Version("1.0-alpha1") shouldBe Version("1.0-a1")
        Version("1.0-beta1") shouldBe Version("1.0-b1")
        Version("1.0-milestone1") shouldBe Version("1.0-m1")
        Version("1.0-rc1") shouldBe Version("1.0-cr1")
    }

    "リリースバージョンの異なる表記が同一と判定されること" {
        Version("1.0-ga") shouldBe Version("1.0")
        Version("1.0-final") shouldBe Version("1.0")
    }

    "修飾子のみ" {
        Version("SomeRandomVersionOne").compareTo(Version("SOMERANDOMVERSIONTWO")) shouldBeLessThan 0
        Version("SomeRandomVersionThree").compareTo(Version("SOMERANDOMVERSIONTWO")) shouldBeLessThan 0
    }

    "区切り文字" {
        Version("1.0alpha1") shouldBe Version("1.0-a1")
        Version("1.0alpha-1") shouldBe Version("1.0-a1")
        Version("1.0beta1") shouldBe Version("1.0-b1")
        Version("1.0beta-1") shouldBe Version("1.0-b1")
        Version("1.0milestone1") shouldBe Version("1.0-m1")
        Version("1.0milestone-1") shouldBe Version("1.0-m1")
        Version("1.0rc1") shouldBe Version("1.0-cr1")
        Version("1.0rc-1") shouldBe Version("1.0-cr1")
        Version("1.0ga") shouldBe Version("1.0")
    }

    "同じではない区切り文字" {
        // 1.0alpha.1 と 1.0-a1(1.0alpha-1)は同一視しない
        Version("1.0alpha.1") shouldNotBe Version("1.0-a1")
    }

    "大文字小文字の区別はしない" {
        Version("1.0ALPHA1") shouldBe Version("1.0-a1")
        Version("1.0Alpha1") shouldBe Version("1.0-a1")
        Version("1.0AlphA1") shouldBe Version("1.0-a1")
        Version("1.0BETA1") shouldBe Version("1.0-b1")
        Version("1.0MILESTONE1") shouldBe Version("1.0-m1")
        Version("1.0RC1") shouldBe Version("1.0-cr1")
        Version("1.0GA") shouldBe Version("1.0-ga")
        Version("1.0FINAL") shouldBe Version("1.0-final")
        Version("1.0SNAPSHOT") shouldBe Version("1-snapshot")
    }

    "長いバージョン" {
        Version("1.0.0.0.0.0.0") shouldBe Version("1")
        Version("1.0.0.0.0.0.0x") shouldBe Version("1x")
    }

    "ダッシュとピリオド" {
        Version("1-0.ga") shouldBe Version("1.0")
        Version("1.0-final") shouldBe Version("1.0")
        Version("1-0-ga") shouldBe Version("1.0")
        Version("1-0-final") shouldBe Version("1-0")
        Version("1-0") shouldBe Version("1.0")
    }

    "filterAfter: 同じバージョンが複数存在" {
        sortedSetOf(Version("2.0.0"), Version("2.0.0")).filterAfter("2.0.0") shouldBe emptyList()
    }
})

object VersionComparator : Comparator<Version> {
    override fun compare(o1: Version, o2: Version): Int {
        return o1.items.compareTo(o2.items)
    }
}
