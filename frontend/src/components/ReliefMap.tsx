"use client";
import { useEffect, useRef } from "react";
import * as maplibregl from "maplibre-gl";
import "maplibre-gl/dist/maplibre-gl.css";
import { ReliefRequest, URGENCY_COLOR } from "@/lib/api";

interface Props {
  items: ReliefRequest[]; selectedId: string | null; onSelect: (id: string) => void;
  picking: boolean; pin: [number, number] | null; onPick: (p: [number, number]) => void;
}

export default function ReliefMap({ items, selectedId, onSelect, picking, pin, onPick }: Props) {
  const box = useRef<HTMLDivElement>(null);
  const map = useRef<maplibregl.Map | null>(null);
  const markers = useRef<maplibregl.Marker[]>([]);
  const pinMarker = useRef<maplibregl.Marker | null>(null);
  const pickState = useRef({ picking, onPick });
  pickState.current = { picking, onPick };

  useEffect(() => {
    const m = new maplibregl.Map({
      container: box.current!, center: [79.92, 6.96], zoom: 11.3, attributionControl: { compact: true },
      style: { version: 8, sources: { osm: { type: "raster", tiles: ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"], tileSize: 256, attribution: "© OpenStreetMap contributors" } }, layers: [{ id: "osm", type: "raster", source: "osm" }] },
    });
    m.addControl(new maplibregl.NavigationControl({ showCompass: false }), "bottom-right");
    m.on("click", (e) => { if (pickState.current.picking) pickState.current.onPick([e.lngLat.lng, e.lngLat.lat]); });
    map.current = m;
    return () => m.remove();
  }, []);

  useEffect(() => {
    markers.current.forEach((x) => x.remove());
    markers.current = items.map((r) => {
      const d = document.createElement("button");
      d.className = "rg-dot" + (r.urgency === "CRITICAL" && r.status !== "FULFILLED" ? " rg-pulse" : "") + (r.id === selectedId ? " rg-sel" : "") + (r.status === "FULFILLED" ? " rg-done" : "");
      d.style.background = URGENCY_COLOR[r.urgency];
      d.setAttribute("aria-label", `${r.urgency} request at ${r.place}`);
      d.onclick = (ev) => { ev.stopPropagation(); onSelect(r.id); };
      return new maplibregl.Marker({ element: d }).setLngLat([r.lng, r.lat]).addTo(map.current!);
    });
  }, [items, selectedId, onSelect]);

  useEffect(() => {
    const r = items.find((i) => i.id === selectedId);
    if (r) map.current?.flyTo({ center: [r.lng, r.lat], zoom: Math.max(map.current.getZoom(), 13), duration: 700 });
  }, [selectedId]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    pinMarker.current?.remove();
    pinMarker.current = pin ? new maplibregl.Marker({ color: "#0F2A2E" }).setLngLat(pin).addTo(map.current!) : null;
  }, [pin]);

  return <div ref={box} className={"h-full w-full " + (picking ? "cursor-crosshair" : "")} />;
}
