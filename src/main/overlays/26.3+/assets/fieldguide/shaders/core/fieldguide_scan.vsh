#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV2;

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out vec2 texCoord0;
layout(location = 2) out float viewRelY;
layout(location = 3) out float scanLimitY;
layout(location = 4) out vec4 scanColor;

void main() {
    vec4 viewPos = ModelViewMat * vec4(Position + ModelOffset, 1.0);
    gl_Position = ProjMat * viewPos;

    vertexColor = Color;
    texCoord0 = UV0;

    viewRelY = Position.y;

    int rawLimit = UV2.x;
    if (rawLimit > 32767) {
        rawLimit -= 65536;
    }
    scanLimitY = float(rawLimit) / 100.0;

    int c = UV2.y;
    float r = float((c >> 12) & 15) / 15.0;
    float g = float((c >> 8) & 15) / 15.0;
    float b = float((c >> 4) & 15) / 15.0;
    float a = float(c & 15) / 15.0;

    scanColor = vec4(r, g, b, a);
}