import {useEffect,useState} from 'react';
import {Activity,Users,ClipboardPlus,Clock,AlertTriangle,CheckCircle2,RefreshCw,HeartPulse,AlertCircle} from 'lucide-react';

const API=import.meta.env.VITE_API_URL||'http://localhost:8082/api';

function effectiveStatus(item){
 if(item.status==='WORKFLOW_STARTED'&&!item.processInstanceKey)return 'LEGACY_UNTRACKED';
 return item.status||'UNKNOWN';
}

function statusLabel(status){
 const labels={ACTIVE:'ACTIVE',COMPLETED:'COMPLETED',INCIDENT:'INCIDENT',TERMINATED:'TERMINATED',SUSPENDED:'SUSPENDED',SUBMITTED:'SUBMITTED',ENGINE_UNAVAILABLE:'ENGINE UNAVAILABLE',WORKFLOW_START_FAILED:'START FAILED',LEGACY_UNTRACKED:'UNTRACKED (OLD RECORD)',UNKNOWN:'UNKNOWN'};
 return labels[status]||status.replaceAll('_',' ');
}

function statusClass(status){
 if(status==='COMPLETED')return 'green';
 if(status==='ACTIVE'||status==='SUSPENDED')return 'blue';
 if(status==='INCIDENT'||status==='TERMINATED'||status==='WORKFLOW_START_FAILED')return 'red';
 return 'amber';
}

export default function App(){
 const [tab,setTab]=useState('new');
 const [items,setItems]=useState([]);
 const [busy,setBusy]=useState(false);
 const [loading,setLoading]=useState(false);
 const [message,setMessage]=useState('');
 const [messageType,setMessageType]=useState('success');
 const [form,setForm]=useState({patientName:'Alex Morgan',age:52,reason:'Recurring chest discomfort',specialists:['Cardiology','Radiology','Dietetics'],consentGiven:true});

 const load=async(showLoading=false)=>{
  if(showLoading)setLoading(true);
  try{
   const response=await fetch(API+'/referrals');
   if(!response.ok)throw new Error('API returned '+response.status);
   setItems(await response.json());
  }catch(error){
   setMessage('Backend unavailable. Check that PatientFlow API is running on port 8082.');
   setMessageType('error');
  }finally{if(showLoading)setLoading(false)}
 };

 useEffect(()=>{load();const timer=setInterval(()=>load(),5000);return()=>clearInterval(timer)},[]);

 const change=(key,value)=>setForm(current=>({...current,[key]:value}));
 const toggle=specialist=>change('specialists',form.specialists.includes(specialist)?form.specialists.filter(value=>value!==specialist):[...form.specialists,specialist]);

 const submit=async event=>{
  event.preventDefault();
  setBusy(true);setMessage('');
  try{
   const response=await fetch(API+'/referrals',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({...form,age:Number(form.age)})});
   const data=await response.json();
   if(!response.ok)throw new Error(data.message||'Referral submission failed');
   setMessage(data.message||'Referral submitted.');
   setMessageType(data.workflowStarted?'success':'warning');
   setTab('dashboard');
   await load();
  }catch(error){setMessage(error.message||'Submission failed. Check the Spring Boot API.');setMessageType('error')}
  finally{setBusy(false)}
 };

 const active=items.filter(item=>effectiveStatus(item)==='ACTIVE').length;
 const completed=items.filter(item=>effectiveStatus(item)==='COMPLETED').length;
 const attention=items.filter(item=>['INCIDENT','TERMINATED','WORKFLOW_START_FAILED','ENGINE_UNAVAILABLE'].includes(effectiveStatus(item))).length;
 const stats=[
  ['Total referrals',items.length,ClipboardPlus],
  ['Active workflows',active,Activity],
  ['Completed workflows',completed,CheckCircle2],
  ['Needs attention',attention,AlertTriangle]
 ];

 return <div className="app">
  <aside>
   <div className="brand"><div className="brandicon"><HeartPulse/></div><div><b>PatientFlow</b><small>REFERRAL & TRIAGE</small></div></div>
   <div className="navlabel">WORKSPACE</div>
   <button className={tab==='dashboard'?'active':''} onClick={()=>setTab('dashboard')}><Activity size={18}/> Overview</button>
   <button className={tab==='new'?'active':''} onClick={()=>setTab('new')}><ClipboardPlus size={18}/> New referral</button>
   <button className={tab==='tasks'?'active':''} onClick={()=>setTab('tasks')}><Users size={18}/> Specialist work queue</button>
   <div className="sidefoot"><span className="dot"/> Demo environment<br/><small>Mock patient data only</small></div>
  </aside>

  <main>
   <header>
    <div><span className="eyebrow">CLINICAL OPERATIONS</span><h1>{tab==='new'?'Create patient referral':tab==='tasks'?'Simulated specialist work queue':'Referral overview'}</h1><p>Coordinate multidisciplinary care through an orchestrated workflow.</p></div>
    <button className="refresh" onClick={()=>load(true)} disabled={loading}><RefreshCw size={16}/> {loading?'Refreshing…':'Refresh'}</button>
   </header>

   {message&&<div className={'notice '+(messageType==='error'?'notice-error':messageType==='warning'?'notice-warning':'')}>{message}</div>}

   {tab==='new'&&<section className="panel">
    <div className="panelhead"><div><h2>Patient information</h2><p>Enter referral details and select specialist departments.</p></div><span className="tag">NEW CASE</span></div>
    <form onSubmit={submit}>
     <div className="fields">
      <label>Patient full name<input required value={form.patientName} onChange={event=>change('patientName',event.target.value)}/></label>
      <label>Age<input required type="number" min="1" max="120" value={form.age} onChange={event=>change('age',event.target.value)}/></label>
     </div>
     <label>Referral reason<textarea required rows="3" value={form.reason} onChange={event=>change('reason',event.target.value)}/></label>
     <div className="fieldtitle">Specialist departments <small>Select one or more</small></div>
     <div className="specialists">{['Cardiology','Radiology','Dietetics'].map(specialist=><label className="check" key={specialist}><input type="checkbox" checked={form.specialists.includes(specialist)} onChange={()=>toggle(specialist)}/><span><b>{specialist}</b><small>{specialist==='Cardiology'?'Heart and circulation':specialist==='Radiology'?'Diagnostic imaging':'Nutrition assessment'}</small></span></label>)}</div>
     <label className="consent"><input type="checkbox" checked={form.consentGiven} onChange={event=>change('consentGiven',event.target.checked)}/> Patient consent recorded for this academic demonstration.</label>
     <button className="primary" disabled={busy||!form.specialists.length}>{busy?'Submitting…':'Submit referral'} <span>→</span></button>
    </form>
   </section>}

   {tab==='dashboard'&&<>
    <div className="stats stats-four">{stats.map(([name,value,Icon])=><div className="stat" key={name}><div className="staticon"><Icon/></div><span>{name}</span><b>{value}</b></div>)}</div>
    <section className="panel"><div className="panelhead"><div><h2>Recent referrals</h2><p>Workflow states refreshed from the Camunda 8.9 process instance API.</p></div></div><ReferralTable items={items}/></section>
    <p className="hint">Older referrals created before process-instance tracking was enabled are labelled as untracked. Their historical Camunda state cannot be reconstructed from the referral record alone.</p>
   </>}

   {tab==='tasks'&&<section className="panel">
    <div className="panelhead"><div><h2>Simulated specialist work queue</h2><p>Referral records routed to specialist departments.</p></div></div>
    <div className="simulation-note"><AlertCircle size={17}/> Specialist consultations are simulated parallel Camunda service tasks. This page is a routing overview, not a human task inbox or clinical approval interface.</div>
    <ReferralTable items={items}/>
   </section>}

   <footer>PatientFlow · Academic workflow prototype · Synthetic data only · Not for clinical use</footer>
  </main>
 </div>;
}

function ReferralTable({items}){
 return <div className="tablewrap"><table><thead><tr><th>Patient</th><th>Referral reason</th><th>Departments</th><th>Workflow status</th></tr></thead><tbody>
  {items.length?items.map(item=>{
   const status=effectiveStatus(item);
   return <tr key={item.id}>
    <td><b>{item.patientName}</b><small>{item.age} years · {String(item.id).slice(0,8)}</small></td>
    <td>{item.reason}</td>
    <td>{item.specialists?.split(',').join(', ')}</td>
    <td><span className={'status '+statusClass(status)}>{status==='COMPLETED'?<CheckCircle2 size={13}/>:status==='INCIDENT'||status==='TERMINATED'||status==='WORKFLOW_START_FAILED'?<AlertTriangle size={13}/>:<Clock size={13}/>} {statusLabel(status)}</span>{item.processInstanceKey&&<small className="process-key">Process key: {item.processInstanceKey}</small>}</td>
   </tr>;
  }):<tr><td colSpan="4" className="empty">No referrals yet. Create your first referral.</td></tr>}
 </tbody></table></div>;
}
