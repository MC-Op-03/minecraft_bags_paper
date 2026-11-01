uint armor3dBallotBitCount(uvec4 mask) {
    uvec4 bits = mask - ((mask >> 1u) & 0x55555555u);
    bits = (bits & 0x33333333u) + ((bits >> 2u) & 0x33333333u);
    bits = (bits + (bits >> 4u)) & 0x0f0f0f0fu;
    bits = (bits * 0x01010101u) >> 24u;
    return bits.x + bits.y + bits.z + bits.w;
}

uint armor3dBallotFindLSB(uvec4 mask) {
    uint word = mask.x != 0u ? 0u : (mask.y != 0u ? 1u : (mask.z != 0u ? 2u : 3u));
    uint bits = mask[word];
    uint lane = word * 32u;
    if ((bits & 0xffffu) == 0u) {
        bits >>= 16u;
        lane += 16u;
    }
    if ((bits & 0xffu) == 0u) {
        bits >>= 8u;
        lane += 8u;
    }
    if ((bits & 0xfu) == 0u) {
        bits >>= 4u;
        lane += 4u;
    }
    if ((bits & 0x3u) == 0u) {
        bits >>= 2u;
        lane += 2u;
    }
    return lane + ((bits & 1u) == 0u ? 1u : 0u);
}

ivec3 armor3dBitfieldExtract(ivec3 value, int offset, int bits) {
    return (value << (32 - offset - bits)) >> (32 - bits);
}

int armor3dBitCount(uint value) {
    return int(armor3dBallotBitCount(uvec4(value, 0u, 0u, 0u)));
}

int armor3dFindLSB(uint value) {
    return value == 0u ? -1 : int(armor3dBallotFindLSB(uvec4(value, 0u, 0u, 0u)));
}

#define subgroupBallotBitCount armor3dBallotBitCount
#define subgroupBallotFindLSB armor3dBallotFindLSB
#define bitfieldExtract armor3dBitfieldExtract
#define bitCount armor3dBitCount
#define findLSB armor3dFindLSB

uint armor3dShuffle(uint value, uint requestedLane) {
    uint result = uint(0);
    for (uint source = 0u; source < gl_SubgroupSize; ++source) {
        uint candidate = subgroupBroadcast(value, source);
        if (requestedLane == source) {
            result = candidate;
        }
    }
    return result;
}

ivec3 armor3dShuffle(ivec3 value, uint requestedLane) {
    ivec3 result = ivec3(0);
    for (uint source = 0u; source < gl_SubgroupSize; ++source) {
        ivec3 candidate = subgroupBroadcast(value, source);
        if (requestedLane == source) {
            result = candidate;
        }
    }
    return result;
}

vec2 armor3dShuffle(vec2 value, uint requestedLane) {
    vec2 result = vec2(0);
    for (uint source = 0u; source < gl_SubgroupSize; ++source) {
        vec2 candidate = subgroupBroadcast(value, source);
        if (requestedLane == source) {
            result = candidate;
        }
    }
    return result;
}

vec3 armor3dShuffle(vec3 value, uint requestedLane) {
    vec3 result = vec3(0);
    for (uint source = 0u; source < gl_SubgroupSize; ++source) {
        vec3 candidate = subgroupBroadcast(value, source);
        if (requestedLane == source) {
            result = candidate;
        }
    }
    return result;
}

#define subgroupShuffle armor3dShuffle
