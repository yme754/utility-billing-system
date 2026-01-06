import { Component, OnInit, ChangeDetectorRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { SidebarComponent } from '../../../shared/components/sidebar/sidebar';
import { NavbarComponent } from '../../../shared/components/navbar/navbar';
import { ConsumerService } from '../../../core/services/consumer';
import { AuthService } from '../../../core/services/auth';
import { finalize, switchMap, map } from 'rxjs/operators';
import { forkJoin, of } from 'rxjs'; 
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartOptions, ChartType } from 'chart.js';

@Component({
  selector: 'app-consumer-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, SidebarComponent, NavbarComponent, BaseChartDirective],
  templateUrl: './consumer-dashboard.html',
  styleUrls: ['./consumer-dashboard.css']
})
export class ConsumerDashboardComponent implements OnInit {
  @ViewChild(BaseChartDirective) chart?: BaseChartDirective;
  isLoading = true;
  username = 'User';  
  consumerId = '';   
  totalDue = 0;
  activeConnections = 0;
  pendingBillsCount = 0;
  today = new Date();
  allBills: any[] = []; 
  recentBills: any[] = [];
  activeTab: string = 'ELECTRICITY';
  tariffs: any[] = [];
  filteredTariffs: any[] = [];

  public lineChartData: ChartConfiguration['data'] = {
    labels: [],
    datasets: [{
      data: [],
      label: 'Monthly Expenses (₹)',
      fill: true,
      tension: 0.4,
      borderColor: '#0d6efd',
      backgroundColor: 'rgba(13, 110, 253, 0.1)',
      pointBackgroundColor: '#fff',
      pointBorderColor: '#0d6efd',
      pointHoverBackgroundColor: '#0d6efd',
      pointHoverBorderColor: '#fff',
      pointRadius: 4,
      pointHoverRadius: 6
    }]
  };
  
  public lineChartOptions: ChartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: { 
        backgroundColor: 'rgba(0,0,0,0.8)',
        padding: 12,
        displayColors: false,
        callbacks: {
          label: (context) => {
            const val = context.parsed?.y || 0;
            return ` ₹ ${val.toLocaleString('en-IN')}`;
          }
        }
      }
    },
    scales: {
      y: { beginAtZero: true, grid: { color: '#f8f9fa' }, ticks: { callback: (value) => '₹' + value } },
      x: { grid: { display: false } }
    }
  };
  public lineChartType: ChartType = 'line';
  public doughnutChartData: ChartConfiguration['data'] = {
    labels: ['Paid', 'Unpaid'],
    datasets: [{ 
      data: [0, 0], 
      backgroundColor: ['#198754', '#dc3545'], 
      hoverBackgroundColor: ['#157347', '#bb2d3b'],
      borderWidth: 2,
      borderColor: '#ffffff',
      hoverOffset: 4
    }]
  };
  
  public doughnutChartOptions: ChartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { position: 'right', labels: { usePointStyle: true, boxWidth: 8 } }
    }
  };
  public doughnutChartType: ChartType = 'doughnut';

  constructor(
    private consumerService: ConsumerService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const user = this.authService.getUserFromStorage();
    if (user && user.userId) {
      this.username = user.username;
      this.initializeDashboard(user.userId);
      this.loadTariffs();
    } else {
      this.authService.logout();
    }
  }

  loadTariffs() {
    this.consumerService.getPublicTariffPlans().subscribe({
      next: (data) => {
        this.tariffs = data || [];
        this.filterTariffs('ELECTRICITY');
      },
      error: (err) => console.error('Failed to load tariffs:', err)
    });
  }

  filterTariffs(type: string) {
    this.activeTab = type;
    this.filteredTariffs = this.tariffs.filter(t => t.utilityType === type);
    this.cdr.detectChanges();
  }

  initializeDashboard(authUserId: string) {
    this.isLoading = true;
    this.consumerService.getProfile(authUserId).pipe(
      switchMap((profile: any) => {
        this.consumerId = profile.id;         
        return this.consumerService.getMyConnections(this.consumerId);
      }),
      switchMap((connections: any[]) => {
        if (!connections || connections.length === 0) {
          this.activeConnections = 0;
          return of([]); 
        }
        this.activeConnections = connections.length;
        const billRequests = connections.map(conn => 
          this.consumerService.getBillsByConnection(conn.id).pipe(map(bills => bills || []))
        );
        return forkJoin(billRequests);
      }),
      finalize(() => {
        this.isLoading = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: (billsArray: any[][]) => {
        this.allBills = billsArray.flat();
        this.processBillingData();
      },
      error: (err) => {
        console.error('Failed to load dashboard data:', err);
        this.isLoading = false;
      }
    });
  }

  processBillingData() {
    this.totalDue = 0;
    this.pendingBillsCount = 0;
    this.allBills.forEach(bill => {
      if (bill.status === 'UNPAID') {
        this.totalDue += (bill.totalAmount || 0);
        this.pendingBillsCount++;
      }
    });
    this.allBills.sort((a, b) => new Date(b.billingDate).getTime() - new Date(a.billingDate).getTime());
    this.recentBills = this.allBills.slice(0, 5);
    this.generateCharts();
  }

  generateCharts() {
    if (this.allBills.length === 0) return;
    const chronologicalBills = [...this.allBills].sort((a, b) => new Date(a.billingDate).getTime() - new Date(b.billingDate).getTime());
    const monthlyData = new Map<string, number>();
    chronologicalBills.forEach(bill => {
      const date = new Date(bill.billingDate);
      const label = date.toLocaleString('default', { month: 'short', year: '2-digit' });
      const currentTotal = monthlyData.get(label) || 0;
      monthlyData.set(label, currentTotal + (bill.totalAmount || 0));
    });
    this.lineChartData.labels = Array.from(monthlyData.keys());
    this.lineChartData.datasets[0].data = Array.from(monthlyData.values());
    const paidCount = this.allBills.filter(b => b.status === 'PAID').length;
    const unpaidCount = this.allBills.filter(b => b.status === 'UNPAID').length;
    this.doughnutChartData.datasets[0].data = [paidCount, unpaidCount];
    if (this.chart) { this.chart.update(); }
  }
}