import{r as o,j as L}from"./vendor-react-D0ZooTGu.js";import{L as r}from"./vendor-leaflet-CpPMzgI9.js";import{w as l,c as $}from"./mapTiles-CSp6YlrY.js";const h={critical:"#ff4d4f",normal:"#52c41a",warning:"#faad14",danger:"#ff4d4f"},R=({buildings:u=[],environmentRecords:g=[],waterStations:m=[],center:x=[32.06,118.79],zoom:v=12})=>{const s=o.useRef(null),d=o.useRef(null),i=o.useRef(r.layerGroup()),a=o.useRef(r.layerGroup()),p=o.useRef(r.layerGroup());return o.useEffect(()=>{if(!d.current||s.current)return;const e=r.map(d.current,{center:l(x[0],x[1]),zoom:v,zoomControl:!0,attributionControl:!0});$().addTo(e),i.current.addTo(e),a.current.addTo(e),p.current.addTo(e);const n={建筑:i.current,环境监测:a.current,水位站:p.current};return r.control.layers(void 0,n,{position:"topright"}).addTo(e),s.current=e,()=>{e.remove(),s.current=null}},[]),o.useEffect(()=>{s.current&&(i.current.clearLayers(),a.current.clearLayers(),p.current.clearLayers(),u.forEach(e=>{const n=r.divIcon({className:"custom-marker",html:`<div style="
          width: 24px; height: 24px;
          border-radius: 4px;
          background: #1890ff;
          border: 2px solid #fff;
          box-shadow: 0 2px 6px rgba(0,0,0,0.3);
          display: flex; align-items: center; justify-content: center;
          font-size: 12px; color: #fff; font-weight: bold;
        ">🏢</div>`,iconSize:[24,24],iconAnchor:[12,12]}),t=r.marker(l(e.lat,e.lng),{icon:n});t.bindPopup(`
        <div style="min-width: 160px;">
          <h3 style="margin: 0 0 8px; font-size: 14px;">${e.name}</h3>
          <p style="margin: 4px 0; font-size: 12px;"><strong>类型:</strong> ${e.type}</p>
          <p style="margin: 4px 0; font-size: 12px;"><strong>面积:</strong> ${e.area} m²</p>
          <p style="margin: 4px 0; font-size: 12px;"><strong>楼层:</strong> ${e.floors}</p>
          <p style="margin: 4px 0; font-size: 12px;"><strong>日均能耗:</strong> ${e.avgDailyConsumption} kWh</p>
        </div>
      `),i.current.addLayer(t)}),g.forEach(e=>{const n=(e.level||"").toLowerCase(),t=h[n]||"#1890ff",f=r.divIcon({className:"custom-marker",html:`<div style="
          width: 28px; height: 28px;
          border-radius: 50%;
          background: ${t};
          border: 3px solid #fff;
          box-shadow: 0 2px 6px rgba(0,0,0,0.3);
          display: flex; align-items: center; justify-content: center;
          font-size: 14px;
        ">📡</div>`,iconSize:[28,28],iconAnchor:[14,14]}),c=r.marker(l(e.lat,e.lng),{icon:f});c.bindPopup(`
        <div style="min-width: 160px;">
          <h3 style="margin: 0 0 8px; font-size: 14px;">${e.stationName}</h3>
          <p style="margin: 4px 0; font-size: 12px;">
            <strong>状态:</strong>
            <span style="color: ${t}; font-weight: 600;">
              ${n==="normal"?"正常":n==="warning"?"警告":"危险"}
            </span>
          </p>
          <p style="margin: 4px 0; font-size: 12px;"><strong>指标:</strong> ${e.metric}</p>
          <p style="margin: 4px 0; font-size: 12px;"><strong>数值:</strong> ${e.value} ${e.unit}</p>
          <p style="margin: 4px 0; font-size: 12px; color: #666;">${e.ts}</p>
        </div>
      `),a.current.addLayer(c)}),m.forEach(e=>{const[n,t]=e.position??[];if(n==null||t==null)return;const f=e.currentLevel>=e.dangerLevel?"danger":e.currentLevel>=e.warningLevel?"warning":"normal",c=h[f],z=r.divIcon({className:"custom-marker",html:`<div style="
          width: 28px; height: 28px;
          border-radius: 50%;
          background: ${c};
          border: 3px solid #fff;
          box-shadow: 0 2px 6px rgba(0,0,0,0.3);
          display: flex; align-items: center; justify-content: center;
          font-size: 14px;
        ">💧</div>`,iconSize:[28,28],iconAnchor:[14,14]}),y=r.marker(l(n,t),{icon:z});y.bindPopup(`
        <div style="min-width: 160px;">
          <h3 style="margin: 0 0 8px; font-size: 14px;">${e.name}</h3>
          <p style="margin: 4px 0; font-size: 12px;"><strong>当前水位:</strong> ${e.currentLevel} m</p>
          <p style="margin: 4px 0; font-size: 12px;"><strong>警戒水位:</strong> ${e.warningLevel} m</p>
          <p style="margin: 4px 0; font-size: 12px;"><strong>危险水位:</strong> ${e.dangerLevel} m</p>
        </div>
      `),p.current.addLayer(y)}))},[u,g,m]),L.jsx("div",{ref:d,style:{width:"100%",height:"100%",minHeight:400,borderRadius:8,overflow:"hidden",zIndex:0}})};export{R as default};
