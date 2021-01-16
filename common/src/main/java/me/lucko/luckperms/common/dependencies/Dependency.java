/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.common.dependencies;

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.common.dependencies.relocation.Relocation;
import me.lucko.luckperms.common.dependencies.relocation.RelocationHelper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * The dependencies used by LuckPerms.
 */
public enum Dependency {

    ASM(
            "org.ow2.asm",
            "asm",
            "7.1",
            "SrL6K20sycyx6qBeoynEB7R7E+0pFfYvjEuMyWJY1N4="
    ),
    ASM_COMMONS(
            "org.ow2.asm",
            "asm-commons",
            "7.1",
            "5VkEidjxmE2Fv+q9Oxc3TFnCiuCdSOxKDrvQGVns01g="
    ),
    JAR_RELOCATOR(
            "me.lucko",
            "jar-relocator",
            "1.4",
            "1RsiF3BiVztjlfTA+svDCuoDSGFuSpTZYHvUK8yBx8I="
    ),

    ADVENTURE(
            "me{}lucko",
            "adventure-api",
            "4.0.1",
            "6dKz0iM9MChLzU5MTLW5HSLFEmmNh/D9Bb6kzJvK/1E=",
            Relocation.of("adventure", "net{}kyori{}adventure")
    ),
    ADVENTURE_PLATFORM(
            "me{}lucko",
            "adventure-platform-api",
            "4.0.1",
            "ie3rz49jg5HU04mWe/f6lXjHt6L+S2SQp/x6YFbhxsc=",
            Relocation.of("adventure", "net{}kyori{}adventure")
    ),
    ADVENTURE_PLATFORM_BUKKIT(
            "me{}lucko",
            "adventure-platform-bukkit",
            "4.0.1",
            "dVbYfUJqmde8jeuTvknCL9hbzqxybal00TELTzQgLbk=",
            Relocation.of("adventure", "net{}kyori{}adventure")
    ),
    ADVENTURE_PLATFORM_BUNGEECORD(
            "me{}lucko",
            "adventure-platform-bungeecord",
            "4.0.1",
            "4phi0TxNKVj5Lko63nlkrd5snIJcaU+cXUfAWsbCX1U=",
            Relocation.of("adventure", "net{}kyori{}adventure")
    ),
    //ADVENTURE_PLATFORM_SPONGEAPI(
    //        "me{}lucko",
    //        "adventure-platform-spongeapi",
    //        "4.0.1",
    //        "6fjWuZMeJ6633RKuZh6sIlMyVIzryQoewONeei2nB+4=",
    //        Relocation.of("adventure", "net{}kyori{}adventure")
    //),
    EVENT(
            "net{}kyori",
            "event-api",
            "3.0.0",
            "yjvdTdAyktl3iFEQFLHC3qYwwt7/DbCd7Zc8Q4SlIag=",
            Relocation.of("eventbus", "net{}kyori{}event")
    ),
    CAFFEINE(
            "com{}github{}ben-manes{}caffeine",
            "caffeine",
            "2.8.4",
            "KV9YN5gQj6b507VJApJpPF5PkCon0DZqAi0T7Ln0lag=",
            Relocation.of("caffeine", "com{}github{}benmanes{}caffeine")
    ),
    OKIO(
            "com{}squareup{}" + RelocationHelper.OKIO_STRING,
            RelocationHelper.OKIO_STRING,
            "1.17.5",
            "Gaf/SNhtPPRJf38lD78pX0MME6Uo3Vt7ID+CGAK4hq0=",
            Relocation.of(RelocationHelper.OKIO_STRING, RelocationHelper.OKIO_STRING)
    ),
    OKHTTP(
            "com{}squareup{}" + RelocationHelper.OKHTTP3_STRING,
            "okhttp",
            "3.14.7",
            "Yg1PpDxcal72JXYCBKiHmeHkpl4ceh2NoC4GHEy7gAA=",
            Relocation.of(RelocationHelper.OKHTTP3_STRING, RelocationHelper.OKHTTP3_STRING),
            Relocation.of(RelocationHelper.OKIO_STRING, RelocationHelper.OKIO_STRING)
    ),
    BYTEBUDDY(
            "net{}bytebuddy",
            "byte-buddy",
            "1.10.9",
            "B7nKbi+XDLA/SyVlHfHy/OJx1JG0TgQJgniHeG9pLU0=",
            Relocation.of("bytebuddy", "net{}bytebuddy")
    ),
    COMMODORE(
            "me{}lucko",
            "commodore",
            "1.7",
            "ncwmvNFfvyZf1Pa0v4fWyMR0Jxe1v1ZgXOiI255IX5Q=",
            Relocation.of("commodore", "me{}lucko{}commodore")
    ),
    MARIADB_DRIVER(
            "org{}mariadb{}jdbc",
            "mariadb-java-client",
            "2.7.0",
            "ABURDun85Q01kf119r4yjDtl5ju9Fg9uV2nXyU3SEdw=",
            Relocation.of("mariadb", "org{}mariadb{}jdbc")
    ),
    MYSQL_DRIVER(
            "mysql",
            "mysql-connector-java",
            "8.0.22",
            "UBne+9EjFilel6bojyqbB/EYNFpOmCcQu6Iy5JmyL08=",
            Relocation.of("mysql", "com{}mysql")
    ),
    POSTGRESQL_DRIVER(
            "org{}postgresql",
            "postgresql",
            "9.4.1212",
            "DLKhWL4xrPIY4KThjI89usaKO8NIBkaHc/xECUsMNl0=",
            Relocation.of("postgresql", "org{}postgresql")
    ),
    H2_DRIVER(
            "com.h2database",
            "h2",
            // seems to be a compat bug in 1.4.200 with older dbs
            // see: https://github.com/h2database/h2database/issues/2078
            "1.4.199",
            "MSWhZ0O8a0z7thq7p4MgPx+2gjCqD9yXiY95b5ml1C4="
            // we don't apply relocations to h2 - it gets loaded via
            // an isolated classloader
    ),
    SQLITE_DRIVER(
            "org.xerial",
            "sqlite-jdbc",
            "3.28.0",
            "k3hOVtv1RiXgbJks+D9w6cG93Vxq0dPwEwjIex2WG2A="
            // we don't apply relocations to sqlite - it gets loaded via
            // an isolated classloader
    ),
    HIKARI(
            "com{}zaxxer",
            "HikariCP",
            "3.4.5",
            "i3MvlHBXDUqEHcHvbIJrWGl4sluoMHEv8fpZ3idd+mE=",
            Relocation.of("hikari", "com{}zaxxer{}hikari")
    ),
    SLF4J_SIMPLE(
            "org.slf4j",
            "slf4j-simple",
            "1.7.30",
            "i5J5y/9rn4hZTvrjzwIDm2mVAw7sAj7UOSh0jEFnD+4="
    ),
    SLF4J_API(
            "org.slf4j",
            "slf4j-api",
            "1.7.30",
            "zboHlk0btAoHYUhcax6ML4/Z6x0ZxTkorA1/lRAQXFc="
    ),
    MONGODB_DRIVER(
            "org.mongodb",
            "mongo-java-driver",
            "3.12.2",
            "eMxHcEtasb/ubFCv99kE5rVZMPGmBei674ZTdjYe58w=",
            Relocation.of("mongodb", "com{}mongodb"),
            Relocation.of("bson", "org{}bson")
    ),
    JEDIS(
            "redis.clients",
            "jedis",
            "3.3.0",
            "HuTfz9xW/mi1fwVQ3xgPmd6qwTRMF/3fyMzw2LmOgy4=",
            Relocation.of("jedis", "redis{}clients{}jedis"),
            Relocation.of("commonspool2", "org{}apache{}commons{}pool2")
    ),
    COMMONS_POOL_2(
            "org.apache.commons",
            "commons-pool2",
            "2.8.0",
            "Xvqfu1SlixoSIFpfrFZfaYKr/rD/Rb28MYdI71/To/8=",
            Relocation.of("commonspool2", "org{}apache{}commons{}pool2")
    ),
    CONFIGURATE_CORE(
            "org{}spongepowered",
            "configurate-core",
            "3.7",
            "V+M3OFm+O0AHsao557kExxa27lYEX7UYE06G/zC/Kyc=",
            Relocation.of("configurate", "ninja{}leaping{}configurate")
    ),
    CONFIGURATE_GSON(
            "org{}spongepowered",
            "configurate-gson",
            "3.7",
            "0JhMGX6mjY8MDCGGc7lrfoHvWbpGiE5R6N3nqJch+SU=",
            Relocation.of("configurate", "ninja{}leaping{}configurate")
    ),
    CONFIGURATE_YAML(
            "org{}spongepowered",
            "configurate-yaml",
            "3.7",
            "14L0JiDuAfQovxkNySeaf9Kul3Nkl0OaW49Ow4ReV8E=",
            Relocation.of("configurate", "ninja{}leaping{}configurate")
    ),
    SNAKEYAML(
            "org.yaml",
            "snakeyaml",
            "1.23",
            "EwCfte3jzyvlqNDxYCFVrqoM5e9fk2aJK9JY2NPU0rE=",
            Relocation.of("yaml", "org{}yaml{}snakeyaml")
    ),
    CONFIGURATE_HOCON(
            "org{}spongepowered",
            "configurate-hocon",
            "3.7",
            "GYdqieCZVgPmoaIFjYN0YHuSVsHO7IsXZrwLAWqCgZM=",
            Relocation.of("configurate", "ninja{}leaping{}configurate"),
            Relocation.of("hocon", "com{}typesafe{}config")
    ),
    HOCON_CONFIG(
            "com{}typesafe",
            "config",
            "1.4.0",
            "qtv9WlJFUb7vENP4kdMFuDuyfVRwPZpN56yioS2YR+I=",
            Relocation.of("hocon", "com{}typesafe{}config")
    ),
    CONFIGURATE_TOML(
            "me{}lucko{}configurate",
            "configurate-toml",
            "3.7",
            "EmyLOfsiR74QGhkktqhexMN8tC3kg1cM1UhM5MCmxuE=",
            Relocation.of("configurate", "ninja{}leaping{}configurate"),
            Relocation.of("toml4j", "com{}moandjiezana{}toml")
    ),
    TOML4J(
            "com{}moandjiezana{}toml",
            "toml4j",
            "0.7.2",
            "9UdeY+fonl22IiNImux6Vr0wNUN3IHehfCy1TBnKOiA=",
            Relocation.of("toml4j", "com{}moandjiezana{}toml")
    );

    private final String mavenRepoPath;
    private final String version;
    private final byte[] checksum;
    private final List<Relocation> relocations;

    private static final String MAVEN_FORMAT = "%s/%s/%s/%s-%s.jar";

    Dependency(String groupId, String artifactId, String version, String checksum) {
        this(groupId, artifactId, version, checksum, new Relocation[0]);
    }

    Dependency(String groupId, String artifactId, String version, String checksum, Relocation... relocations) {
        this.mavenRepoPath = String.format(MAVEN_FORMAT,
                rewriteEscaping(groupId).replace(".", "/"),
                rewriteEscaping(artifactId),
                version,
                rewriteEscaping(artifactId),
                version
        );
        this.version = version;
        this.checksum = Base64.getDecoder().decode(checksum);
        this.relocations = ImmutableList.copyOf(relocations);
    }

    private static String rewriteEscaping(String s) {
        return s.replace("{}", ".");
    }

    public String getFileName() {
        return name().toLowerCase().replace('_', '-') + "-" + this.version;
    }

    String getMavenRepoPath() {
        return this.mavenRepoPath;
    }

    public byte[] getChecksum() {
        return this.checksum;
    }

    public boolean checksumMatches(byte[] hash) {
        return Arrays.equals(this.checksum, hash);
    }

    public List<Relocation> getRelocations() {
        return this.relocations;
    }

    /**
     * Creates a {@link MessageDigest} suitable for computing the checksums
     * of dependencies.
     *
     * @return the digest
     */
    public static MessageDigest createDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}
