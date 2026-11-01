#version 330

#define ARMOR3D_ITEM_ALPHA_CUTOFF 0.1
#define ARMOR3D_EMISSIVE_ALPHA_MIN 0.996
#define ARMOR3D_EMISSIVE_ALPHA_MAX 0.997

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec4 lightMapColor;
in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 textureColor = texture(Sampler0, texCoord);
    bool isEmissive = textureColor.a > ARMOR3D_EMISSIVE_ALPHA_MIN
        && textureColor.a < ARMOR3D_EMISSIVE_ALPHA_MAX;
    vec4 finalColor = textureColor * vertexColor * ColorModulator;

    if (!isEmissive) {
        finalColor *= lightMapColor;
    }

    if (finalColor.a < ARMOR3D_ITEM_ALPHA_CUTOFF) {
        discard;
    }

    fragColor = finalColor;
}
