import React, { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import Navbar from './components/Navbar';
import Sidebar from './components/Sidebar';
import Login from './pages/Login';
import DashboardOverview from './pages/DashboardOverview';
import Transactions from './pages/Transactions';
import FraudMonitoring from './pages/FraudMonitoring';
import Incidents from './pages/Incidents';
import IncidentInvestigation from './pages/IncidentInvestigation';
import FraudRulesManagement from './pages/FraudRulesManagement';

const DashboardContent = () => {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState('overview');
  const [selectedIncident, setSelectedIncident] = useState(null);

  if (!user) {
    return <Login />;
  }

  const handleInvestigate = (target) => {
    setSelectedIncident(target);
    setActiveTab('investigation');
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans selection:bg-sky-500 selection:text-white">
      <Navbar />
      
      <div className="flex-1 flex overflow-hidden">
        <Sidebar activeTab={activeTab} setActiveTab={setActiveTab} />
        
        <main className="flex-1 overflow-y-auto p-6 md:p-8 max-w-7xl mx-auto w-full">
          {activeTab === 'overview' && (
            <DashboardOverview 
              setActiveTab={setActiveTab} 
              onInvestigate={handleInvestigate} 
            />
          )}

          {activeTab === 'transactions' && (
            <Transactions />
          )}

          {activeTab === 'fraud' && (
            <FraudMonitoring 
              onInvestigate={handleInvestigate} 
            />
          )}

          {activeTab === 'incidents' && (
            <Incidents 
              onInvestigate={handleInvestigate} 
            />
          )}

          {activeTab === 'investigation' && (
            <IncidentInvestigation 
              incident={selectedIncident} 
              onBack={() => setActiveTab('incidents')} 
            />
          )}

          {activeTab === 'rules' && (
            <FraudRulesManagement />
          )}
        </main>
      </div>
    </div>
  );
};

const App = () => {
  return (
    <AuthProvider>
      <DashboardContent />
    </AuthProvider>
  );
};

export default App;
