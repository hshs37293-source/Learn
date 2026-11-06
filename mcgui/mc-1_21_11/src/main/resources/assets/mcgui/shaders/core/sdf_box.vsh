#version 330

// Wierzchołek elementu SDF: przekazuje kolory narożników (interpolowane bilinearnie) i spakowane parametry.
// Układ atrybutów = SdfPipeline.FORMAT (Position, UV0, Color, UV1, UV2, LineWidth).
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec3 Position;
in vec2 UV0;       // 0..1 w obrębie quada
in vec4 Color;     // kolor narożnika
in ivec2 UV1;      // rozmiar quada, jednostka 0.25 px
in ivec2 UV2;      // promienie rogów: x = tl | tr<<7, y = br | bl<<7, jednostka 0.5 px
in float LineWidth; // >0 obrys (grubość px), <0 cień (-promień rozmycia px), 0 wypełnienie

out vec2 vUv;
out vec4 vColor;
flat out ivec2 vSize;
flat out ivec2 vRadii;
out float vLine;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vUv = UV0;
    vColor = Color;
    vSize = UV1;
    vRadii = UV2;
    vLine = LineWidth;
}
