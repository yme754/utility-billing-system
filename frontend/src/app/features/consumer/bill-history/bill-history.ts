import { Component, Input, OnInit, OnChanges, SimpleChanges, ChangeDetectorRef } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ConsumerService } from '../../../core/services/consumer';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { SidebarComponent } from '../../../shared/components/sidebar/sidebar';
import { NavbarComponent } from '../../../shared/components/navbar/navbar';
import { AuthService } from '../../../core/services/auth';
import { jsPDF } from 'jspdf';
import { switchMap } from 'rxjs/operators';
import { forkJoin } from 'rxjs';

declare var window: any;

@Component({
  selector: 'app-bill-history',
  standalone: true,
  imports: [CommonModule, SidebarComponent, NavbarComponent, FormsModule],
  providers: [DatePipe],
  templateUrl: './bill-history.html',
  styleUrls: ['./bill-history.css'] 
})
export class BillHistoryComponent implements OnInit, OnChanges {
  @Input() connectionId!: string;
  
  allBills: any[] = [];
  filteredBills: any[] = [];
  isLoading = false;
  selectedUtility = 'ALL';
  filterStatus = 'ALL';
  selectedBill: any = null;
  paymentMode = 'UPI';
  isProcessingPayment = false;
  paymentSuccessData: any = null;
  private bootstrapModal: any;

  constructor(
    private consumerService: ConsumerService,
    private authService: AuthService,
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    private datePipe: DatePipe
  ) {}

  ngOnInit() {
    if (this.connectionId) { this.fetchBillsByConnection(this.connectionId); } 
    else { this.fetchAllUserBills(); }
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['connectionId'] && this.connectionId) { this.fetchBillsByConnection(this.connectionId); }
  }
  fetchBillsByConnection(connId: string) {
    this.isLoading = true;
    this.consumerService.getBillsByConnection(connId).subscribe({
      next: (data) => this.handleBillsData(data),
      error: () => { this.isLoading = false; this.cdr.detectChanges(); }
    });
  }
  fetchAllUserBills() {
    this.isLoading = true;
    const user = this.authService.getUserFromStorage();
    if (user && user.userId) {
      this.consumerService.getProfile(user.userId).pipe(
        switchMap((profile: any) => this.consumerService.getMyConnections(profile.id))
      ).subscribe({
        next: (connections: any[]) => {
           if (!connections || connections.length === 0) { this.handleBillsData([]); return; }
           const connMap = new Map();
           connections.forEach(c => connMap.set(c.id, c.utilityType));
           const billRequests = connections.map(conn => this.consumerService.getBillsByConnection(conn.id));
           forkJoin(billRequests).subscribe({
             next: (responses: any[]) => {
               const allBills = responses.flat().map(bill => ({
                 ...bill,
                 utilityType: connMap.get(bill.connectionId) || bill.utilityName || 'SERVICE' 
               }));
               this.handleBillsData(allBills);
             },
             error: () => { this.isLoading = false; this.cdr.detectChanges(); }
           });
        },
        error: (err) => { this.isLoading = false; this.cdr.detectChanges(); }
      });
    }
  }

  handleBillsData(data: any[]) {
    this.allBills = data.sort((a, b) => new Date(b.billingDate).getTime() - new Date(a.billingDate).getTime());
    this.applyFilters();
    this.isLoading = false;
    this.cdr.detectChanges();
  }

  setUtilityFilter(type: string) {
    this.selectedUtility = type;
    this.applyFilters();
  }

  applyFilters() {
    this.filteredBills = this.allBills.filter(bill => {
      const matchStatus = this.filterStatus === 'ALL' ? true : bill.status === this.filterStatus;
      const billUtility = (bill.utilityType || '').toUpperCase();
      const matchUtility = this.selectedUtility === 'ALL' ? true : billUtility === this.selectedUtility;
      return matchStatus && matchUtility;
    });
    this.cdr.detectChanges();
  }

  openPaymentModal(bill: any) {
    this.selectedBill = bill; 
    this.paymentMode = 'UPI'; 
    this.paymentSuccessData = null;
    
    const modalEl = document.getElementById('paymentModal');
    if (modalEl) {
      if (window.bootstrap) {
        this.bootstrapModal = new window.bootstrap.Modal(modalEl);
        this.bootstrapModal.show();
      } else {
        alert('Bootstrap JS not loaded. Please refresh.');
      }
    }
  }

  selectMode(mode: string) { this.paymentMode = mode; }
  
  processPayment() {
    if (!this.selectedBill) return;
    const token = this.authService.getToken(); 
    if (!token) {
      alert("You are logged out. Please login again.");
      return;
    }
    this.isProcessingPayment = true;
    let backendPaymentMode = this.paymentMode;
    if (this.paymentMode === 'CARD') {
        backendPaymentMode = 'CREDIT_CARD'; 
    }

    const payload = { 
        billId: this.selectedBill.id, 
        paymentMode: backendPaymentMode
    };

    const headers = { Authorization: `Bearer ${token}` };
    
    this.http.post('http://localhost:8080/payments/pay', payload, { headers }).subscribe({
      next: (res: any) => {
        this.isProcessingPayment = false;        
        this.paymentSuccessData = { 
          transactionId: res.transactionId || 'TXN-' + Math.floor(Math.random() * 1000000), 
          date: new Date(), 
          amount: this.selectedBill.totalAmount, 
          mode: this.paymentMode
        };
        this.selectedBill.status = 'PAID';
        this.selectedBill.paymentDate = new Date();
        this.selectedBill.paymentMode = this.paymentMode;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isProcessingPayment = false;
        console.error('Backend Error:', err);        
        if (err.status === 500) {
           alert('Payment Failed: Server Error. Please check if "PaymentMode" matches in Backend Enums.');
        } else {
           alert('Payment Failed. Please try again.');
        }
        this.cdr.detectChanges();
      }
    });
  }

  closeModal() {
    if (this.bootstrapModal) {
      this.bootstrapModal.hide();
    } else {
      const modalEl = document.getElementById('paymentModal');
      const modalInstance = window.bootstrap?.Modal?.getInstance(modalEl);
      if (modalInstance) modalInstance.hide();
    }
    if (!this.connectionId) this.applyFilters();
  }

  downloadReceipt(bill: any) {
    const doc = new jsPDF();
    const pageWidth = doc.internal.pageSize.getWidth();
    const centerX = pageWidth / 2;
    doc.setFillColor(37, 99, 235); doc.rect(0, 0, pageWidth, 40, 'F');
    doc.setFontSize(22); doc.setTextColor(255, 255, 255); doc.text('PAYMENT RECEIPT', centerX, 25, { align: 'center' });
    doc.setTextColor(0, 0, 0); doc.setFontSize(11);
    let y = 60;
    const addLine = (label: string, value: string) => { doc.setTextColor(100); doc.text(label, 20, y); doc.setTextColor(0); doc.text(value, 80, y); y += 10; };
    addLine('Transaction Date:', this.datePipe.transform(new Date(), 'medium') || '');
    addLine('Bill ID:', `#${bill.id}`);
    addLine('Utility Service:', bill.utilityType || 'N/A');
    addLine('Units Consumed:', `${bill.unitsConsumed} Units`);
    if(bill.paymentMode) addLine('Payment Mode:', bill.paymentMode);
    y += 10; doc.setFillColor(240, 255, 244); doc.setDrawColor(25, 135, 84); doc.roundedRect(20, y, 170, 25, 3, 3, 'FD');
    doc.setFontSize(14); doc.setTextColor(25, 135, 84); doc.text(`Total Amount Paid: Rs. ${bill.totalAmount}`, centerX, y + 17, { align: 'center' });
    doc.save(`Receipt_${bill.id}.pdf`);
  }
}