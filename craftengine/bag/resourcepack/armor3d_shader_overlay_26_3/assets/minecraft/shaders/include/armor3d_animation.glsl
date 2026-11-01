#define ARMOR3D_TEXTURE_ANIMATION

ivec4 armor3dAnimationRead(int pixel) {
    int width = textureSize(Sampler0, 0).x;
    return ivec4(texelFetch(Sampler0, ivec2(pixel % width, pixel / width), 0) * 255.0 + 0.5);
}

int armor3dAnimationPointer(ivec4 value) {
    return value.r * 65536 + value.g * 256 + value.b;
}

ivec2 armor3dAnimationPair(ivec4 value) {
    return ivec2(value.r * 256 + value.g, value.b * 256 + value.a);
}

vec2 armor3dAnimationMasksForFrame(int frame, int baseY, float height) {
    ivec2 emissive = armor3dAnimationPair(armor3dAnimationRead(frame + 1));
    ivec2 tint = armor3dAnimationPair(armor3dAnimationRead(frame + 2));
    return vec2(emissive.x == 65535 ? -2.0 : float(emissive.y - baseY) / height,
        tint.x == 65535 ? -2.0 : float(tint.y - baseY) / height);
}

void armor3dAnimate(ivec4 header, int face, vec2 uv, ivec2 dimensions, vec2 jitter) {
    int clip = armor3dAnimationPointer(armor3dAnimationRead(armor3dAnimationPointer(header) + face));
    if (clip == 0) {
        return;
    }
    ivec2 origin = armor3dAnimationPair(armor3dAnimationRead(clip));
    ivec4 sequence = armor3dAnimationRead(clip + 2);
    ivec2 timing = armor3dAnimationPair(armor3dAnimationRead(clip + 3));
    int frames = armor3dAnimationPointer(armor3dAnimationRead(clip + 4));
    int steps = armor3dAnimationPointer(sequence);
    float time = mod(GameTime * 24000.0, float(timing.x));
    int low = 0;
    int high = timing.y;
    for (int iteration = 0; iteration < 16; ++iteration) {
        if (low < high) {
            int middle = (low + high) / 2;
            int end = armor3dAnimationPair(armor3dAnimationRead(steps + middle)).y;
            if (time < float(end)) {
                high = middle;
            } else {
                low = middle + 1;
            }
        }
    }
    ivec2 current = armor3dAnimationPair(armor3dAnimationRead(steps + low));
    int next = armor3dAnimationPair(armor3dAnimationRead(steps + (low + 1) % timing.y)).x;
    int start = low == 0 ? 0 : armor3dAnimationPair(armor3dAnimationRead(steps + low - 1)).y;
    int currentFrame = frames + current.x * 3;
    int nextFrame = frames + next * 3;
    ivec2 currentOrigin = armor3dAnimationPair(armor3dAnimationRead(currentFrame));
    ivec2 nextOrigin = armor3dAnimationPair(armor3dAnimationRead(nextFrame));
    vec2 atlasSize = vec2(textureSize(Sampler0, 0));
    vec2 localUv = uv * vec2(dimensions) - vec2(origin);
    texCoord = (vec2(currentOrigin) + localUv) / atlasSize + jitter;
    armor3dNextTexCoord = (vec2(nextOrigin) + localUv) / atlasSize + jitter;
    armor3dFrameBlend = sequence.a != 0 ? (time - float(start)) / float(current.y - start) : 0.0;
    armor3dAnimationMasks = vec4(
        armor3dAnimationMasksForFrame(currentFrame, currentOrigin.y, atlasSize.y),
        armor3dAnimationMasksForFrame(nextFrame, nextOrigin.y, atlasSize.y));
    armor3dLayerFlags |= 4;
}
